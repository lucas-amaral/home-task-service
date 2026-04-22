package com.amaral.hometask.service;

import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.dtos.AssignmentDto
import com.amaral.hometask.model.dtos.WeekSummaryDto
import com.amaral.hometask.repository.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
class WeekSummaryService(
   private val assignmentRepo: AssignmentRepository,
   private val familyConfigService: FamilyConfigService,
   private val pointLedgerService: PointLedgerService
) {

    fun getWeekSummary(weekStart: LocalDate): WeekSummaryDto {
        val cfg = familyConfigService.getFamilyConfig()
        val assignments = assignmentRepo.findAllForWeek(weekStart, weekStart.plusDays(7))
        return WeekSummaryDto(
                weekStart = weekStart,
                child1Name = cfg.child1Name, child2Name = cfg.child2Name,
                assignments = assignments.map { it.toDto() },
        points = pointLedgerService.weekPointsMap(weekStart)
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
        points = task.points
    )
}
