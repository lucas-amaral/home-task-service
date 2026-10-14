package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.PushSubscription
import com.amaral.hometask.model.ReminderLog
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.model.dtos.FamilyConfigDto
import com.amaral.hometask.repository.PushSubscriptionRepository
import com.amaral.hometask.repository.ReminderLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderServiceTest {

    private val boardService: BoardService = mock()
    private val subscriptions: PushSubscriptionRepository = mock()
    private val reminderLog: ReminderLogRepository = mock()
    private val sender: PushSender = mock()
    private val familyConfigService: FamilyConfigService = mock()
    private val service = ReminderService(boardService, subscriptions, reminderLog, sender, familyConfigService)

    private val tuesday = LocalDate.of(2024, 1, 16)
    private val sunday = LocalDate.of(2024, 1, 21)
    private val claraPhone = PushSubscription(id = 1, endpoint = "https://push/clara", p256dh = "k", auth = "a", assignee = Assignee.CHILD1)
    private val bernardoPhone = PushSubscription(id = 2, endpoint = "https://push/bernardo", p256dh = "k", auth = "a", assignee = Assignee.CHILD2)

    private fun task(name: String, frequency: TaskFrequency = TaskFrequency.DAILY) =
        Task(id = name.hashCode().toLong(), name = name, type = TaskType.DAILY, frequency = frequency)

    private fun daily(name: String, to: Assignee, date: LocalDate = tuesday, done: Boolean = false) =
        Assignment(task = task(name), assignedTo = to, periodDate = date, completedAt = if (done) LocalDateTime.now() else null)

    @BeforeEach
    fun setup() {
        whenever(familyConfigService.getFamilyConfig()).thenReturn(FamilyConfigDto(child1Name = "Clara", child2Name = "Bernardo"))
        whenever(subscriptions.findByAssignee(Assignee.CHILD1)).thenReturn(listOf(claraPhone))
        whenever(subscriptions.findByAssignee(Assignee.CHILD2)).thenReturn(listOf(bernardoPhone))
        whenever(sender.configured).thenReturn(true)
        whenever(sender.send(any(), any())).thenReturn(PushResult.SENT)
    }

    @Test
    fun `each child hears about their open tasks, the joint ones and the unclaimed ones, not what is done`() {
        whenever(boardService.ensureAssignments(tuesday)).thenReturn(
            listOf(
                daily("Lixo", Assignee.CHILD1),
                daily("Aspirador", Assignee.UNASSIGNED),
                daily("Mesa do almoço", Assignee.BOTH),
                daily("Banheiro", Assignee.CHILD2, done = true),
            ),
        )

        val result = service.send(ReminderSlot.NOON, tuesday)

        verify(sender).send(claraPhone, PushMessage("Tarefas de hoje", "Clara, faltam 3 tarefas: Lixo, Aspirador e Mesa do almoço."))
        verify(sender).send(bernardoPhone, PushMessage("Tarefas de hoje", "Bernardo, faltam 2 tarefas: Aspirador e Mesa do almoço."))
        assertEquals(mapOf("Clara" to 1, "Bernardo" to 1), result.notified)
        verify(reminderLog).save(argThat<ReminderLog> { date == tuesday && slot == "NOON" })
    }

    @Test
    fun `a child with nothing left gets no notification`() {
        whenever(boardService.ensureAssignments(tuesday)).thenReturn(listOf(daily("Lixo", Assignee.CHILD1)))

        service.send(ReminderSlot.EVENING, tuesday)

        verify(sender).send(argThat { assignee == Assignee.CHILD1 }, any())
        verify(sender, never()).send(argThat { assignee == Assignee.CHILD2 }, any())
    }

    @Test
    fun `weekly tasks join only on sunday, their last day`() {
        val weekly = Assignment(task = task("Banheiro", TaskFrequency.WEEKLY), assignedTo = Assignee.CHILD2, periodWeek = LocalDate.of(2024, 1, 15))
        whenever(boardService.ensureAssignments(tuesday)).thenReturn(listOf(weekly))
        whenever(boardService.ensureAssignments(sunday)).thenReturn(listOf(weekly))

        service.send(ReminderSlot.NOON, tuesday)
        verify(sender, never()).send(any(), any())

        service.send(ReminderSlot.NOON, sunday)
        verify(sender).send(bernardoPhone, PushMessage("Tarefas de hoje", "Bernardo, falta 1 tarefa: Banheiro."))
    }

    @Test
    fun `a reminder already sent that day is not sent again`() {
        whenever(reminderLog.existsByDateAndSlot(tuesday, "NOON")).thenReturn(true)

        val result = service.send(ReminderSlot.NOON, tuesday)

        assertTrue(result.alreadySent)
        verify(boardService, never()).ensureAssignments(any())
        verify(sender, never()).send(any(), any())
    }

    @Test
    fun `a device the browser dropped is forgotten`() {
        whenever(boardService.ensureAssignments(tuesday)).thenReturn(listOf(daily("Lixo", Assignee.CHILD1)))
        whenever(sender.send(claraPhone, PushMessage("Tarefas de hoje", "Clara, falta 1 tarefa: Lixo."))).thenReturn(PushResult.GONE)

        service.send(ReminderSlot.NOON, tuesday)

        verify(subscriptions).delete(claraPhone)
    }
}
