package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.model.dtos.AssignmentDto
import com.amaral.hometask.repository.AssignmentRepository
import com.amaral.hometask.repository.PointLedgerRepository
import com.amaral.hometask.repository.TaskRepository
import com.amaral.hometask.util.DateTimeUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class AssignmentService(
    private val assignmentRepo: AssignmentRepository,
    private val taskRepo: TaskRepository,
    private val ledgerRepo: PointLedgerRepository,
    private val familyConfigService: FamilyConfigService
) {

    // ── Board operations ─────────────────────────────────────────────────────

    fun ensureDailyAssignment(task: Task, date: LocalDate): Assignment {
        assignmentRepo.upsertDaily(task.id, task.defaultAssignee.name, date)
        return assignmentRepo.findAllByTaskIdAndPeriodDate(task.id, date).first()
    }

    fun ensureWeeklyAssignment(task: Task, weekStart: LocalDate): Assignment {
        assignmentRepo.upsertWeekly(task.id, task.defaultAssignee.name, weekStart)
        return assignmentRepo.findAllByTaskIdAndPeriodWeek(task.id, weekStart).first()
    }

    // ── Assignment management ────────────────────────────────────────────────

    @Transactional
    fun assignTask(req: AssignRequest): AssignmentDto {
        require(req.date != null || req.weekStart != null) {
            "Either date or weekStart must be provided"
        }
        val task = taskRepo.findById(req.taskId)
            .orElseThrow { NoSuchElementException("Task ${req.taskId} not found") }

        val existing = if (req.date != null)
            assignmentRepo.findAllByTaskIdAndPeriodDate(task.id, req.date).firstOrNull()
        else
            assignmentRepo.findAllByTaskIdAndPeriodWeek(task.id, req.weekStart!!).firstOrNull()

        val assignment = if (existing != null) {
            if (existing.penaltyApplied) {
                val week = weekStart(existing.displayDate)
                resolvePersons(existing.assignedTo)
                    .forEach { addLedger(it, week, +1, "Reassigned, penalty reversed: ${existing.task.name}") }
            }
            assignmentRepo.save(
                existing.copy(
                    assignedTo = req.assignedTo,
                    completedAt = null,
                    bonusEarned = false,
                    penaltyApplied = false,
                    missedDeadline = false,
                    deleted = false
                )
            )
        } else {
            assignmentRepo.save(
                Assignment(task = task, assignedTo = req.assignedTo,
                    periodDate = req.date, periodWeek = req.weekStart)
            )
        }
        return assignment.toDto()
    }

    /**
     * Punitive model: completing a task on time and correctly is simply the
     * expected baseline — it earns no points. Points only ever move down,
     * via [applyMissedDeadlinePenalties] or a manual [applyPenalty] for a
     * task done late/incomplete/not done. So marking complete here just
     * records the fact; if a penalty was already applied for this exact
     * assignment (e.g. it was completed *after* being flagged missed), we
     * leave that penalty in place — completing late doesn't undo the
     * occurrence, matching the "prazo perdido = ocorrência" rule.
     */
    @Transactional
    fun completeAssignment(id: Long, req: CompleteRequest): AssignmentDto {
        val assignment = findAssignment(id)
        check(assignment.completedAt == null) { "Assignment already completed" }
        return assignmentRepo.save(assignment.copy(completedAt = LocalDateTime.now())).toDto()
    }

    @Transactional
    fun uncompleteAssignment(id: Long): AssignmentDto {
        val assignment = findAssignment(id)
        if (assignment.completedAt == null) return assignment.toDto()
        return assignmentRepo.save(assignment.copy(completedAt = null)).toDto()
    }

    @Transactional
    fun applyPenalty(id: Long): AssignmentDto {
        val assignment = findAssignment(id)
        check(!assignment.missedDeadline) {
            "Cannot manually remove a missed-deadline penalty (applied by the scheduler)"
        }
        // Same rule as the deadline scheduler: nobody having claimed the task
        // isn't a loophole — it falls on both children.
        val effectiveAssignee = if (assignment.assignedTo == Assignee.UNASSIGNED) Assignee.BOTH else assignment.assignedTo
        val week = weekStart(assignment.displayDate)
        resolvePersons(effectiveAssignee)
            .forEach { addLedger(it, week, -1, "Manual penalty: ${assignment.task.name}") }
        return assignmentRepo.save(assignment.copy(assignedTo = effectiveAssignee, penaltyApplied = true)).toDto()
    }

    @Transactional
    fun removeManualPenalty(id: Long): AssignmentDto {
        val assignment = findAssignment(id)
        check(assignment.penaltyApplied) { "No penalty to remove on assignment $id" }
        check(!assignment.missedDeadline) {
            "Cannot remove a missed-deadline penalty (applied automatically by the scheduler)"
        }
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo)
            .forEach { addLedger(it, week, +1, "Penalty reversed: ${assignment.task.name}") }
        return assignmentRepo.save(assignment.copy(penaltyApplied = false)).toDto()
    }

    /**
     * Feature 1 — Delete an assignment entirely.
     *
     * If a penalty (occurrence) had been recorded on it, that penalty is
     * reversed in the ledger before deletion so the week totals stay accurate.
     * One-off task assignments also deactivate the parent task so it won't
     * show up anywhere else.
     */
    @Transactional
    fun deleteAssignment(id: Long) {
        val assignment = findAssignment(id)

        // Reverse the −1 occurrence if a penalty had been recorded on this
        // assignment — deleting it entirely means it should no longer count.
        if (assignment.penaltyApplied) {
            val week = weekStart(assignment.displayDate)
            resolvePersons(assignment.assignedTo)
                .forEach { addLedger(it, week, +1, "Assignment deleted, penalty reversed: ${assignment.task.name}") }
        }

        if (assignment.task.oneOff) {
            // One-off tasks should fully disappear after deletion.
            taskRepo.save(assignment.task.copy(active = false))
            assignmentRepo.deleteById(id)
            return
        }

        // Recurring tasks keep a tombstone for this period so board refreshes
        // do not immediately recreate the assignment.
        assignmentRepo.save(
            assignment.copy(
                completedAt = null,
                bonusEarned = false,
                penaltyApplied = false,
                missedDeadline = false,
                deleted = true
            )
        )
    }

    // ── Missed deadline penalties ────────────────────────────────────────────

    @Transactional
    fun applyMissedDeadlinePenalties(date: LocalDate = DateTimeUtils.today()): Int {
        val week = weekStart(date)
        val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY

        val candidates = assignmentRepo.findMissedCandidates(date, week)
            .filter { a ->
                when (a.task.frequency) {
                    TaskFrequency.DAILY, TaskFrequency.EVERY_2_DAYS -> true
                    else -> isSunday
                }
            }

        candidates.forEach { a ->
            // Nobody claimed it before the deadline: the responsibility falls on
            // both children equally, per house rule — it's actually reassigned
            // to BOTH (not just penalized as if it were), so the board reflects
            // what happened.
            val effectiveAssignee = if (a.assignedTo == Assignee.UNASSIGNED) Assignee.BOTH else a.assignedTo

            assignmentRepo.save(
                a.copy(assignedTo = effectiveAssignee, missedDeadline = true, penaltyApplied = true)
            )
            resolvePersons(effectiveAssignee)
                .forEach { person -> addLedger(person, week, -1, "Missed deadline: ${a.task.name}") }
        }

        return candidates.size
    }

    /**
     * Feature 3 — Returns assignments whose deadline has passed in the current
     * hour but are not yet completed, not already penalised, and have a valid
     * HH:mm deadline string.
     *
     * Called by [DeadlineNotificationScheduler] at XX:05 every hour.
     */
    fun findOverdueForNotification(date: LocalDate, hour: Int): List<Assignment> {
        val week = weekStart(date)
        return assignmentRepo.findMissedCandidates(date, week).filter { a ->
            val dl = a.task.deadline.trim()
            if (dl.isBlank()) return@filter false
            try {
                val parts = dl.split(":")
                val dlHour = parts[0].toInt()
                // Fire when the deadline hour matches the current check hour
                dlHour == hour
            } catch (_: Exception) { false }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun findAssignment(id: Long): Assignment =
        assignmentRepo.findById(id).orElseThrow { NoSuchElementException("Assignment $id not found") }

    private fun addLedger(person: Assignee, week: LocalDate, delta: Int, reason: String) {
        ledgerRepo.save(PointLedger(assignee = person, weekStart = week, delta = delta, reason = reason))
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun resolvePersons(assignee: Assignee): List<Assignee> = when (assignee) {
        Assignee.BOTH       -> listOf(Assignee.CHILD1, Assignee.CHILD2)
        Assignee.UNASSIGNED -> emptyList()
        else                -> listOf(assignee)
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    fun Assignment.toDto() = AssignmentDto(
        id = id, taskId = task.id, taskName = task.name,
        taskDescription = task.description,
        taskType = task.type, taskFrequency = task.frequency,
        assignedTo = assignedTo, periodDate = displayDate,
        completed = completedAt != null, completedAt = completedAt,
        bonusEarned = bonusEarned, penaltyApplied = penaltyApplied,
        points = task.points
    )
}
