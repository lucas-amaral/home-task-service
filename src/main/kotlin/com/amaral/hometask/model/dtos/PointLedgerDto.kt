package com.amaral.hometask.model.dtos

import com.amaral.hometask.model.Assignee
import java.time.LocalDate

data class PointLedgerDto(
    val assignee: Assignee,
    val weekStart: LocalDate,
    val total: Int
)
