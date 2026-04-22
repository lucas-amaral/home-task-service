package com.amaral.hometask.model.dtos

import java.time.LocalDate

data class WeekSummaryDto(
    val weekStart: LocalDate,
    val child1Name: String,
    val child2Name: String,
    val assignments: List<AssignmentDto>,
    val points: Map<String, Int>
)
