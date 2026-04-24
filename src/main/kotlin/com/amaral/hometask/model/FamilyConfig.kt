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
    val child2Name: String = "Child 2",

    /**
     * WhatsApp phone numbers for Callmebot notifications.
     * Format: international, digits only — e.g. "5554999990000"
     * Leave blank to disable notifications for that child.
     */
    val child1Phone: String? = "",
    val child2Phone: String? = ""
)
