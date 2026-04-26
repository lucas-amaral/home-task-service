package com.amaral.hometask.model.dtos

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import java.time.LocalDate
import java.time.LocalDateTime

data class AssignmentDto(
    val id: Long,
    val taskId: Long,
    val taskName: String,
    var taskDescription: String,
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
