package com.amaral.hometask.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "family_config")
data class FamilyConfig(
    @Id
    val id: Long = 1,

    val child1Name: String = "Child 1",
    val child2Name: String = "Child 2"
)
