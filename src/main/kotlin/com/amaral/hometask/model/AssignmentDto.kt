package com.amaral.hometask.model

import java.time.LocalDate
import java.time.LocalDateTime

data class AssignmentDto(
    val id: Long,
    val taskId: Long,
    val taskName: String,
    val taskType: TaskType,
    val taskFrequency: TaskFrequency,
    val assignedTo: Assignee,
    /** Exact date for daily tasks, Monday for weekly tasks */
    val periodDate: LocalDate,
    val completed: Boolean,
    val completedAt: LocalDateTime?,
    val bonusEarned: Boolean,
    val penaltyApplied: Boolean,
    val points: Int
)
