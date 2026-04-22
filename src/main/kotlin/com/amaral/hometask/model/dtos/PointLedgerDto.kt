package com.amaral.hometask.model

import java.time.LocalDate

data class PointLedgerDto(
    val assignee: Assignee,
    val weekStart: LocalDate,
    val total: Int
)
