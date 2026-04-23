package com.amaral.hometask.model.requests

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import java.time.LocalDateTime

data class CreateTaskRequest(
    val name: String,
    val description: String = "",
    val type: TaskType,
    val frequency: TaskFrequency,
    val defaultAssignee: Assignee = Assignee.UNASSIGNED,
    val points: Int = 1,
    val timeWindow: String = "",
    val deadline: String = "",
    val deadlineDate: LocalDateTime? = null,
    val sortOrder: Int = 0
)
