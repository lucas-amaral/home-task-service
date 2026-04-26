package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.model.dtos.AssignmentDto
import com.amaral.hometask.model.dtos.BoardDto
import com.amaral.hometask.repository.AssignmentRepository
import com.amaral.hometask.repository.TaskRepository
import com.amaral.hometask.util.DateTimeUtils
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BoardService(
    private val taskRepo: TaskRepository,
    private val assignmentRepo: AssignmentRepository,
    private val assignmentService: AssignmentService,
    private val familyConfigService: FamilyConfigService,
    private val pointLedgerService: PointLedgerService
) {

    /**
     * Returns today's daily + this week's weekly assignments.
     *
     * One-off tasks (oneOff=true) are intentionally excluded here —
     * they manage their own single Assignment row created at task-creation time
     * and must NOT be re-inserted by the board on subsequent days.
     */
    @Transactional
    fun getBoard(date: LocalDate = DateTimeUtils.today()): BoardDto {
        val week = DateTimeUtils.weekStart(date)
        val cfg  = familyConfigService.getFamilyConfig()

        // Only recurring tasks drive automatic assignment creation
        val recurringTasks = taskRepo.findByActiveTrueOrderBySortOrderAsc()
            .filter { !it.oneOff }

        val assignments = recurringTasks.flatMap { task ->
            when (task.frequency) {
                TaskFrequency.DAILY    -> assignmentService.ensureDailyAssignment(task, date).toVisibleList()
                TaskFrequency.WEEKLY   -> assignmentService.ensureWeeklyAssignment(task, week).toVisibleList()
                TaskFrequency.BIWEEKLY -> {
                    val weekNumber = week.dayOfYear / 7
                    if (weekNumber % 2 == 0) assignmentService.ensureWeeklyAssignment(task, week).toVisibleList() else emptyList()
                }
                TaskFrequency.MONTHLY  -> {
                    if (week.dayOfMonth <= 7) assignmentService.ensureWeeklyAssignment(task, week).toVisibleList() else emptyList()
                }
            }
        }

        // Also include any one-off assignments for today (created at task-creation time)
        val oneOffToday = assignmentRepo.findByPeriodDate(date)
            .filter { it.task.oneOff }

        return BoardDto(
            date = date, weekStart = week,
            child1Name = cfg.child1Name, child2Name = cfg.child2Name,
            assignments = (assignments + oneOffToday).map { it.toDto() },
            weekPoints  = pointLedgerService.weekPointsMap(week)
        )
    }

    private fun Assignment?.toVisibleList(): List<Assignment> =
        if (this != null && this.deleted != true) listOf(this) else emptyList()

    private fun Assignment.toDto() = AssignmentDto(
        id = id, taskId = task.id, taskName = task.name,
        taskDescription = task.description,
        taskType = task.type, taskFrequency = task.frequency,
        assignedTo = assignedTo, periodDate = displayDate,
        completed = completedAt != null, completedAt = completedAt,
        bonusEarned = bonusEarned, penaltyApplied = penaltyApplied,
        points = task.points
    )
}
