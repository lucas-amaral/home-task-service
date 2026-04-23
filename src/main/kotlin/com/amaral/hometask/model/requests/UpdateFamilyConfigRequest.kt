package com.amaral.hometask.model.requests

data class UpdateFamilyConfigRequest(
    val child1Name: String,
    val child2Name: String,
    val child1Phone: String = "",
    val child2Phone: String = ""
)
