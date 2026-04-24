package com.amaral.hometask.model.dtos

data class FamilyConfigDto(
    val child1Name: String,
    val child2Name: String,
    val child1Phone: String? = null,
    val child2Phone: String? = null
)
