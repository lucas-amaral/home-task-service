package com.amaral.hometask.model

import java.time.LocalDate

data class AssignRequest(
    val taskId: Long,
    val assignedTo: Assignee,
    /** For daily tasks; omit for weekly */
    val date: LocalDate? = null,
    /** For weekly tasks; omit for daily */
    val weekStart: LocalDate? = null
)
