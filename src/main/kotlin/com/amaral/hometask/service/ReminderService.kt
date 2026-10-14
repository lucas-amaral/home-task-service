package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.ReminderLog
import com.amaral.hometask.repository.PushSubscriptionRepository
import com.amaral.hometask.repository.ReminderLogRepository
import com.amaral.hometask.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The two reminders of the day and the hours each one may still go out: a reminder missed while the
 * service slept is sent when it wakes up, but never after its window (no notifications late at night).
 */
enum class ReminderSlot(val title: String, val from: LocalTime, val until: LocalTime) {
    NOON("Tarefas de hoje", LocalTime.of(12, 0), LocalTime.of(19, 0)),
    EVENING("Ainda dá tempo: tarefas pendentes", LocalTime.of(19, 0), LocalTime.of(22, 0));

    companion object {
        /** The reminder due at [time], if any. */
        fun dueAt(time: LocalTime): ReminderSlot? = entries.firstOrNull { !time.isBefore(it.from) && time.isBefore(it.until) }
    }
}

/** What a reminder run did. */
data class ReminderResult(val date: LocalDate, val slot: ReminderSlot, val alreadySent: Boolean, val notified: Map<String, Int>)

/**
 * Tells each child, on their own devices, which tasks of the day are still open. A child's tasks are
 * the ones assigned to them, the joint ones and the unclaimed ones (an unclaimed task left undone
 * falls on both). Weekly tasks join on Sunday, their last day.
 */
@Service
class ReminderService(
    private val boardService: BoardService,
    private val subscriptions: PushSubscriptionRepository,
    private val reminderLog: ReminderLogRepository,
    private val pushSender: PushSender,
    private val familyConfigService: FamilyConfigService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Sends [slot] for [date], once: a second call for the same date and slot does nothing. */
    @Transactional
    fun send(slot: ReminderSlot, date: LocalDate = DateTimeUtils.today()): ReminderResult {
        if (reminderLog.existsByDateAndSlot(date, slot.name)) return ReminderResult(date, slot, true, emptyMap())
        val open = boardService.ensureAssignments(date).filter { it.isOpenOn(date) }
        val config = familyConfigService.getFamilyConfig()
        val notified = mapOf(Assignee.CHILD1 to config.child1Name, Assignee.CHILD2 to config.child2Name).mapNotNull { (child, name) ->
            val tasks = open.filter { it.assignedTo == child || it.assignedTo == Assignee.BOTH || it.assignedTo == Assignee.UNASSIGNED }
            if (tasks.isEmpty()) return@mapNotNull null
            val message = PushMessage(slot.title, messageFor(name, tasks.map { it.task.name }))
            val devices = subscriptions.findByAssignee(child)
            devices.forEach { device ->
                if (pushSender.send(device, message) == PushResult.GONE) subscriptions.delete(device)
            }
            name to devices.size
        }.toMap()
        if (pushSender.configured) reminderLog.save(ReminderLog(date = date, slot = slot.name))
        log.info("Reminder {} of {}: {}", slot, date, notified)
        return ReminderResult(date, slot, false, notified)
    }

    private fun Assignment.isOpenOn(date: LocalDate): Boolean {
        if (completedAt != null || deleted == true || missedDeadline) return false
        // A weekly task is only due on the last day of its week.
        return periodDate == date || (periodWeek != null && date.dayOfWeek == DayOfWeek.SUNDAY)
    }

    companion object {
        /** "Clara, faltam 3 tarefas: Lixo, Aspirador e Mesa do jantar." */
        fun messageFor(name: String, tasks: List<String>): String {
            val list = if (tasks.size == 1) tasks.single() else tasks.dropLast(1).joinToString(", ") + " e " + tasks.last()
            return if (tasks.size == 1) "$name, falta 1 tarefa: $list." else "$name, faltam ${tasks.size} tarefas: $list."
        }
    }
}
