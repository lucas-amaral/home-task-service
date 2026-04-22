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
            if (existing.completedAt != null) reversePoints(existing)
            assignmentRepo.save(existing.copy(assignedTo = req.assignedTo, completedAt = null, bonusEarned = false))
        } else {
            assignmentRepo.save(
                Assignment(task = task, assignedTo = req.assignedTo,
                    periodDate = req.date, periodWeek = req.weekStart)
            )
        }
        return assignment.toDto()
    }

    @Transactional
    fun completeAssignment(id: Long, req: CompleteRequest): AssignmentDto {
        val assignment = findAssignment(id)
        check(assignment.completedAt == null) { "Assignment already completed" }
        val updated = assignmentRepo.save(
            assignment.copy(completedAt = LocalDateTime.now(), bonusEarned = req.bonusEarned)
        )
        awardPoints(updated)
        return updated.toDto()
    }

    @Transactional
    fun uncompleteAssignment(id: Long): AssignmentDto {
        val assignment = findAssignment(id)
        if (assignment.completedAt == null) return assignment.toDto()
        reversePoints(assignment)
        return assignmentRepo.save(assignment.copy(completedAt = null, bonusEarned = false)).toDto()
    }

    @Transactional
    fun applyPenalty(id: Long): AssignmentDto {
        val assignment = findAssignment(id)
        check(!assignment.missedDeadline) {
            "Cannot manually remove a missed-deadline penalty (applied by the scheduler)"
        }
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo)
            .forEach { addLedger(it, week, -1, "Manual penalty: ${assignment.task.name}") }
        return assignmentRepo.save(assignment.copy(penaltyApplied = true)).toDto()
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

    // ── Missed deadline penalties ────────────────────────────────────────────

    @Transactional
    fun applyMissedDeadlinePenalties(date: LocalDate = DateTimeUtils.today()): Int {
        val week = weekStart(date)
        val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY

        val candidates = assignmentRepo.findMissedCandidates(date, week)
            .filter { a ->
                when (a.task.frequency) {
                    TaskFrequency.DAILY -> true
                    else -> isSunday
                }
            }

        candidates.forEach { a ->
            assignmentRepo.save(a.copy(missedDeadline = true, penaltyApplied = true))
            resolvePersons(a.assignedTo)
                .forEach { person -> addLedger(person, week, -1, "Missed deadline: ${a.task.name}") }
        }

        return candidates.size
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun findAssignment(id: Long): Assignment =
        assignmentRepo.findById(id).orElseThrow { NoSuchElementException("Assignment $id not found") }

    private fun awardPoints(assignment: Assignment) {
        val pts = assignment.task.points + if (assignment.bonusEarned) 1 else 0
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo)
            .forEach { addLedger(it, week, pts, "Completed: ${assignment.task.name}") }
    }

    private fun reversePoints(assignment: Assignment) {
        val pts = assignment.task.points + if (assignment.bonusEarned) 1 else 0
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo)
            .forEach { addLedger(it, week, -pts, "Reversed: ${assignment.task.name}") }
    }

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

    private fun Assignment.toDto() = AssignmentDto(
        id = id,
        taskId = task.id,
        taskName = task.name,
        taskDescription = task.description,
        taskType = task.type,
        taskFrequency = task.frequency,
        assignedTo = assignedTo,
        periodDate = displayDate,
        completed = completedAt != null,
        completedAt = completedAt,
        bonusEarned = bonusEarned,
        penaltyApplied = penaltyApplied,
        points = task.points
    )
}

