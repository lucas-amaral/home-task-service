package com.amaral.hometask.model.dtos

import java.time.LocalDate

data class BoardDto(
    val date: LocalDate,
    val weekStart: LocalDate,
    /** Child names resolved from config */
    val child1Name: String,
    val child2Name: String,
    /** Today's daily assignments + this week's weekly assignments */
    val assignments: List<AssignmentDto>,
    /** Accumulated points per child this week (≤ 0 in the punitive model) */
    val weekPoints: Map<String, Int>,
    /** Occurrence counts + active consequence ladder per child this week */
    val weeklyStatus: List<WeeklyStatusDto> = emptyList()
)