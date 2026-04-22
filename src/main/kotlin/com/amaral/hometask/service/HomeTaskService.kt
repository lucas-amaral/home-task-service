package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.model.dtos.*
import com.amaral.hometask.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class HomeTaskService(
    private val taskRepo: TaskRepository,
    private val assignmentRepo: AssignmentRepository,
    private val ledgerRepo: PointLedgerRepository,
    private val rewardRepo: RewardRepository,
    private val familyConfigRepo: FamilyConfigRepository
) {

    // ── Date helpers ─────────────────────────────────────────────────────────

    fun today(): LocalDate = LocalDate.now()

    fun weekStart(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // ── Family config ────────────────────────────────────────────────────────

    fun getFamilyConfig(): FamilyConfigDto {
        val cfg = familyConfigRepo.findById(1L).orElse(FamilyConfig())
        return FamilyConfigDto(cfg.child1Name, cfg.child2Name)
    }

    @Transactional
    fun updateFamilyConfig(req: UpdateFamilyConfigRequest): FamilyConfigDto {
        val cfg = FamilyConfig(id = 1L, child1Name = req.child1Name.trim(), child2Name = req.child2Name.trim())
        familyConfigRepo.save(cfg)
        return FamilyConfigDto(cfg.child1Name, cfg.child2Name)
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    fun listTasks(): List<TaskDto> =
        taskRepo.findByActiveTrueOrderBySortOrderAsc().map { it.toDto() }

    @Transactional
    fun createTask(req: CreateTaskRequest): TaskDto =
        taskRepo.save(
            Task(
                name = req.name, description = req.description,
                type = req.type, frequency = req.frequency,
                defaultAssignee = req.defaultAssignee,
                points = req.points, timeWindow = req.timeWindow,
                deadline = req.deadline, sortOrder = req.sortOrder
            )
        ).toDto()

    // ── Board ────────────────────────────────────────────────────────────────

    /**
     * Returns today's daily + this week's weekly assignments.
     *
     * Duplicate prevention strategy:
     *   1. Try native INSERT ... ON CONFLICT DO NOTHING (idempotent upsert).
     *   2. Then SELECT to return the existing/newly created row.
     *
     * This eliminates race conditions entirely — even if two requests arrive
     * simultaneously, only one row is ever inserted per (task × period).
     */
    @Transactional
    fun getBoard(date: LocalDate = today()): BoardDto {
        val week = weekStart(date)
        val cfg  = getFamilyConfig()
        val tasks = taskRepo.findByActiveTrueOrderBySortOrderAsc()

        val assignments = tasks.flatMap { task ->
            when (task.frequency) {
                TaskFrequency.DAILY    -> listOf(ensureDailyAssignment(task, date))
                TaskFrequency.WEEKLY   -> listOf(ensureWeeklyAssignment(task, week))
                TaskFrequency.BIWEEKLY -> {
                    val weekNumber = week.dayOfYear / 7
                    if (weekNumber % 2 == 0) listOf(ensureWeeklyAssignment(task, week)) else emptyList()
                }
                TaskFrequency.MONTHLY  -> {
                    if (week.dayOfMonth <= 7) listOf(ensureWeeklyAssignment(task, week)) else emptyList()
                }
            }
        }

        return BoardDto(
            date = date, weekStart = week,
            child1Name = cfg.child1Name, child2Name = cfg.child2Name,
            assignments = assignments.map { it.toDto() },
            weekPoints  = weekPointsMap(week)
        )
    }

    private fun ensureDailyAssignment(task: Task, date: LocalDate): Assignment {
        // Step 1: idempotent insert — does nothing if row already exists
        assignmentRepo.upsertDaily(task.id, task.defaultAssignee.name, date)
        // Step 2: return the (possibly pre-existing) row — always exactly one
        return assignmentRepo.findAllByTaskIdAndPeriodDate(task.id, date).first()
    }

    private fun ensureWeeklyAssignment(task: Task, weekStart: LocalDate): Assignment {
        assignmentRepo.upsertWeekly(task.id, task.defaultAssignee.name, weekStart)
        return assignmentRepo.findAllByTaskIdAndPeriodWeek(task.id, weekStart).first()
    }

    // ── Week summary ─────────────────────────────────────────────────────────

    fun getWeekSummary(weekStart: LocalDate): WeekSummaryDto {
        val cfg = getFamilyConfig()
        val assignments = assignmentRepo.findAllForWeek(weekStart, weekStart.plusDays(7))
        return WeekSummaryDto(
            weekStart = weekStart,
            child1Name = cfg.child1Name, child2Name = cfg.child2Name,
            assignments = assignments.map { it.toDto() },
            points = weekPointsMap(weekStart)
        )
    }

    // ── Assignments ──────────────────────────────────────────────────────────

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
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo)
            .forEach { addLedger(it, week, -1, "Manual penalty: ${assignment.task.name}") }
        return assignmentRepo.save(assignment.copy(penaltyApplied = true)).toDto()
    }

    // ── Points ───────────────────────────────────────────────────────────────

    fun getPointsHistory(): List<PointLedgerDto> =
        ledgerRepo.findAllOrderByWeekDesc()
            .groupBy { it.weekStart to it.assignee }
            .map { (key, entries) -> PointLedgerDto(key.second, key.first, entries.sumOf { it.delta }) }
            .sortedByDescending { it.weekStart }

    // ── Rewards ──────────────────────────────────────────────────────────────

    fun listRewards(): List<RewardDto> =
        rewardRepo.findByActiveTrue().map { RewardDto(it.id, it.name, it.pointsCost, it.emoji) }

    // ── Cron: missed-deadline penalties ──────────────────────────────────────

    /**
     * Called at 23:30 every day by [DeadlinePenaltyScheduler].
     *
     * Rules:
     *   - Daily assignments that are not completed and not already penalised → −1 pt
     *   - Weekly assignments are penalised only on Sunday night (end of the week)
     *   - UNASSIGNED and BOTH assignments are skipped (no one to debit)
     *
     * Returns the number of penalties applied (useful for logging / testing).
     */
    @Transactional
    fun applyMissedDeadlinePenalties(date: LocalDate = today()): Int {
        val week = weekStart(date)
        val isSunday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY

        val candidates = assignmentRepo.findMissedCandidates(date, week)
            .filter { a ->
                when (a.task.frequency) {
                    // Daily tasks are penalised every night
                    TaskFrequency.DAILY -> true
                    // Weekly/Biweekly/Monthly: only penalise at end of week (Sunday)
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

    private fun weekPointsMap(week: LocalDate): Map<String, Int> {
        val entries = ledgerRepo.findByWeekStart(week)
        return listOf(Assignee.CHILD1, Assignee.CHILD2).associate { person ->
            person.name to maxOf(0, entries.filter { it.assignee == person }.sumOf { it.delta })
        }
    }

    private fun resolvePersons(assignee: Assignee): List<Assignee> = when (assignee) {
        Assignee.BOTH       -> listOf(Assignee.CHILD1, Assignee.CHILD2)
        Assignee.UNASSIGNED -> emptyList()
        else                -> listOf(assignee)
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun Task.toDto() = TaskDto(
        id, name, description, type, frequency, defaultAssignee,
        points, timeWindow, deadline, active, sortOrder
    )

    private fun Assignment.toDto() = AssignmentDto(
        id = id, taskId = task.id,
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
