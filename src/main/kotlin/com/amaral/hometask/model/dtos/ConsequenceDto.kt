package com.amaral.hometask.model.dtos

/**
 * One rung of the fixed weekly consequence ladder (see [ConsequenceLadder]).
 * `active` = true once the child's occurrence count for the week has
 * reached this level — consequences are cumulative, so at 4 occurrences
 * levels 1–4 are all active.
 */
data class ConsequenceDto(
    val level: Int,
    val description: String,
    val active: Boolean
)
