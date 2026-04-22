package com.amaral.hometask.model

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
    val active: Boolean,
    val sortOrder: Int
)
