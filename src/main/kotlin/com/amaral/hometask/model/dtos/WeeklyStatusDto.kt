package com.amaral.hometask.model.dtos

import com.amaral.hometask.model.Assignee
import java.time.LocalDate

data class WeeklyStatusDto(
    val assignee: Assignee,
    val name: String,
    val weekStart: LocalDate,
    /** Total −1 occurrences this week (not-done, late, or incomplete tasks), net of any manual reversals. */
    val occurrenceCount: Int,
    /** The fixed 6-step consequence ladder, each marked active/inactive for this child this week. */
    val consequences: List<ConsequenceDto>
)
