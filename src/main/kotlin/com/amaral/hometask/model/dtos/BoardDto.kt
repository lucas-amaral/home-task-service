package com.amaral.hometask.model.dtos

import com.amaral.hometask.model.AssignmentDto
import java.time.LocalDate

data class BoardDto(
    val date: LocalDate,
    val weekStart: LocalDate,
    /** Child names resolved from config */
    val child1Name: String,
    val child2Name: String,
    /** Today's daily assignments + this week's weekly assignments */
    val assignments: List<AssignmentDto>,
    /** Accumulated points per child this week */
    val weekPoints: Map<String, Int>
)