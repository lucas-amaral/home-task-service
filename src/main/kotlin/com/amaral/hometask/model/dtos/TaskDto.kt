package com.amaral.hometask.model.dtos

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import java.time.LocalDateTime

data class TaskDto(
    val id: Long,
    val name: String,
    val description: String,
    val type: TaskType,
    val frequency: TaskFrequency,
    val defaultAssignee: Assignee,
    val points: Int,
    val timeWindow: String,
    val deadline: String,
    val deadlineDate: LocalDateTime?,
    val active: Boolean,
    val sortOrder: Int
)
