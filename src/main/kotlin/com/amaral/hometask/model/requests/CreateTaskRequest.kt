package com.amaral.hometask.model

data class CreateTaskRequest(
    val name: String,
    val description: String = "",
    val type: TaskType,
    val frequency: TaskFrequency,
    val defaultAssignee: Assignee = Assignee.UNASSIGNED,
    val points: Int = 1,
    val timeWindow: String = "",
    val deadline: String = "",
    val sortOrder: Int = 0
)
