package com.amaral.hometask.service

import com.amaral.hometask.model.AssignRequest
import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.AssignmentDto
import com.amaral.hometask.model.BoardDto
import com.amaral.hometask.model.CompleteRequest
import com.amaral.hometask.model.CreateTaskRequest
import com.amaral.hometask.model.FamilyConfig
import com.amaral.hometask.model.FamilyConfigDto
import com.amaral.hometask.model.PointLedger
import com.amaral.hometask.model.PointLedgerDto
import com.amaral.hometask.model.RewardDto
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskDto
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.UpdateFamilyConfigRequest
import com.amaral.hometask.model.WeekSummaryDto
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

    // ── Date helpers ────────────────────────────────────────────────────────

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

    // ── Board ─────────────────────────────────────────────────────────────────

    /**
     * Returns today's daily assignments + this week's weekly assignments.
     * Creates missing Assignment rows on the fly (auto-populate from defaultAssignee).
     */
    @Transactional
    fun getBoard(date: LocalDate = today()): BoardDto {
        val week = weekStart(date)
        val cfg = getFamilyConfig()
        val tasks = taskRepo.findByActiveTrueOrderBySortOrderAsc()

        val assignments = tasks.flatMap { task ->
            when (task.frequency) {
                TaskFrequency.DAILY ->
                    listOf(ensureDailyAssignment(task, date))
                TaskFrequency.WEEKLY ->
                    listOf(ensureWeeklyAssignment(task, week))
                TaskFrequency.BIWEEKLY -> {
                    // show only on even weeks (rough heuristic)
                    val weekNumber = week.dayOfYear / 7
                    if (weekNumber % 2 == 0) listOf(ensureWeeklyAssignment(task, week)) else emptyList()
                }
                TaskFrequency.MONTHLY -> {
                    // show only on the first week of each month
                    if (week.dayOfMonth <= 7) listOf(ensureWeeklyAssignment(task, week)) else emptyList()
                }
            }
        }

        val weekPoints = calculateWeekPoints(week)

        return BoardDto(
            date = date,
            weekStart = week,
            child1Name = cfg.child1Name,
            child2Name = cfg.child2Name,
            assignments = assignments.map { it.toDto() },
            weekPoints = mapOf(
                "CHILD1" to (weekPoints[Assignee.CHILD1] ?: 0),
                "CHILD2" to (weekPoints[Assignee.CHILD2] ?: 0)
            )
        )
    }

    private fun ensureDailyAssignment(task: Task, date: LocalDate): Assignment {
        return assignmentRepo.findByTaskIdAndPeriodDate(task.id, date)
            ?: assignmentRepo.save(
                Assignment(
                    task = task,
                    assignedTo = task.defaultAssignee,
                    periodDate = date
                )
            )
    }

    private fun ensureWeeklyAssignment(task: Task, weekStart: LocalDate): Assignment {
        return assignmentRepo.findByTaskIdAndPeriodWeek(task.id, weekStart)
            ?: assignmentRepo.save(
                Assignment(
                    task = task,
                    assignedTo = task.defaultAssignee,
                    periodWeek = weekStart
                )
            )
    }

    // ── Week summary (history page) ──────────────────────────────────────────

    fun getWeekSummary(weekStart: LocalDate): WeekSummaryDto {
        val cfg = getFamilyConfig()
        val weekEnd = weekStart.plusDays(7)
        val assignments = assignmentRepo.findAllForWeek(weekStart, weekEnd)
        val points = calculateWeekPoints(weekStart)

        return WeekSummaryDto(
            weekStart = weekStart,
            child1Name = cfg.child1Name,
            child2Name = cfg.child2Name,
            assignments = assignments.map { it.toDto() },
            points = mapOf(
                "CHILD1" to (points[Assignee.CHILD1] ?: 0),
                "CHILD2" to (points[Assignee.CHILD2] ?: 0)
            )
        )
    }

    // ── Assignments ──────────────────────────────────────────────────────────

    @Transactional
    fun assignTask(req: AssignRequest): AssignmentDto {
        require(req.date != null || req.weekStart != null) { "Either date or weekStart must be provided" }

        val task = taskRepo.findById(req.taskId)
            .orElseThrow { NoSuchElementException("Task ${req.taskId} not found") }

        val existing = if (req.date != null)
            assignmentRepo.findByTaskIdAndPeriodDate(task.id, req.date)
        else
            assignmentRepo.findByTaskIdAndPeriodWeek(task.id, req.weekStart!!)

        val assignment = if (existing != null) {
            // If already completed, subtract old points before re-assigning
            if (existing.completedAt != null) {
                reversePoints(existing)
            }
            assignmentRepo.save(existing.copy(assignedTo = req.assignedTo, completedAt = null, bonusEarned = false))
        } else {
            assignmentRepo.save(
                Assignment(
                    task = task,
                    assignedTo = req.assignedTo,
                    periodDate = req.date,
                    periodWeek = req.weekStart
                )
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
        val updated = assignmentRepo.save(assignment.copy(completedAt = null, bonusEarned = false))
        return updated.toDto()
    }

    @Transactional
    fun applyPenalty(id: Long): AssignmentDto {
        val assignment = findAssignment(id)
        val week = weekStart(assignment.displayDate)
        val targets = resolvePersons(assignment.assignedTo)
        targets.forEach { addLedger(it, week, -1, "Penalty: ${assignment.task.name}") }
        val updated = assignmentRepo.save(assignment.copy(penaltyApplied = true))
        return updated.toDto()
    }

    // ── Points ───────────────────────────────────────────────────────────────

    fun getPointsHistory(): List<PointLedgerDto> {
        val raw = ledgerRepo.findAllOrderByWeekDesc()
        // Aggregate by week+assignee
        return raw.groupBy { it.weekStart to it.assignee }
            .map { (key, entries) ->
                PointLedgerDto(key.second, key.first, entries.sumOf { it.delta })
            }
            .sortedByDescending { it.weekStart }
    }

    // ── Rewards ──────────────────────────────────────────────────────────────

    fun listRewards(): List<RewardDto> =
        rewardRepo.findByActiveTrue().map { RewardDto(it.id, it.name, it.pointsCost, it.emoji) }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun findAssignment(id: Long): Assignment =
        assignmentRepo.findById(id).orElseThrow { NoSuchElementException("Assignment $id not found") }

    private fun awardPoints(assignment: Assignment) {
        val pts = assignment.task.points + if (assignment.bonusEarned) 1 else 0
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo).forEach { person ->
            addLedger(person, week, pts, "Complete: ${assignment.task.name}")
        }
    }

    private fun reversePoints(assignment: Assignment) {
        val pts = assignment.task.points + if (assignment.bonusEarned) 1 else 0
        val week = weekStart(assignment.displayDate)
        resolvePersons(assignment.assignedTo).forEach { person ->
            addLedger(person, week, -pts, "Reversed: ${assignment.task.name}")
        }
    }

    private fun addLedger(person: Assignee, week: LocalDate, delta: Int, reason: String) {
        ledgerRepo.save(PointLedger(assignee = person, weekStart = week, delta = delta, reason = reason))
    }

    private fun calculateWeekPoints(week: LocalDate): Map<Assignee, Int> {
        val entries = ledgerRepo.findByWeekStart(week)
        return entries
            .filter { it.assignee in listOf(Assignee.CHILD1, Assignee.CHILD2) }
            .groupBy { it.assignee }
            .mapValues { (_, v) -> maxOf(0, v.sumOf { it.delta }) }
    }

    private fun resolvePersons(assignee: Assignee): List<Assignee> = when (assignee) {
        Assignee.BOTH -> listOf(Assignee.CHILD1, Assignee.CHILD2)
        Assignee.UNASSIGNED -> emptyList()
        else -> listOf(assignee)
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun Task.toDto() = TaskDto(
        id, name, description, type, frequency, defaultAssignee,
        points, timeWindow, deadline, active, sortOrder
    )

    private fun Assignment.toDto() = AssignmentDto(
        id = id, taskId = task.id, taskName = task.name,
        taskType = task.type, taskFrequency = task.frequency,
        assignedTo = assignedTo,
        periodDate = displayDate,
        completed = completedAt != null,
        completedAt = completedAt,
        bonusEarned = bonusEarned,
        penaltyApplied = penaltyApplied,
        points = task.points
    )
}
