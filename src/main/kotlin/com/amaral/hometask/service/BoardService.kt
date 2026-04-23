package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.model.dtos.AssignmentDto
import com.amaral.hometask.model.dtos.BoardDto
import com.amaral.hometask.model.dtos.WeekSummaryDto
import com.amaral.hometask.repository.AssignmentRepository
import com.amaral.hometask.repository.TaskRepository
import com.amaral.hometask.util.DateTimeUtils
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class BoardService(
    private val taskRepo: TaskRepository,
    private val assignmentService: AssignmentService,
    private val familyConfigService: FamilyConfigService,
    private val pointLedgerService: PointLedgerService
) {

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
    fun getBoard(date: LocalDate = DateTimeUtils.today()): BoardDto {
        val week = DateTimeUtils.weekStart(date)
        val cfg = familyConfigService.getFamilyConfig()
        val tasks = taskRepo.findByActiveTrueOrderBySortOrderAsc()

        val assignments = tasks.flatMap { task ->
            when (task.frequency) {
                TaskFrequency.DAILY    -> listOf(assignmentService.ensureDailyAssignment(task, date))
                TaskFrequency.WEEKLY   -> listOf(assignmentService.ensureWeeklyAssignment(task, week))
                TaskFrequency.BIWEEKLY -> {
                    val weekNumber = week.dayOfYear / 7
                    if (weekNumber % 2 == 0) listOf(assignmentService.ensureWeeklyAssignment(task, week)) else emptyList()
                }
                TaskFrequency.MONTHLY  -> {
                    if (week.dayOfMonth <= 7) listOf(assignmentService.ensureWeeklyAssignment(task, week)) else emptyList()
                }
            }
        }

        return BoardDto(
            date = date, weekStart = week,
            child1Name = cfg.child1Name, child2Name = cfg.child2Name,
            assignments = assignments.map { it.toDto() },
            weekPoints = pointLedgerService.weekPointsMap(week)
        )
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
        points = task.points,
        deadlineDate = task.deadlineDate
    )
}

