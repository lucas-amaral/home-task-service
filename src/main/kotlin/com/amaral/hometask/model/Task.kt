package com.amaral.hometask.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tasks")
data class Task(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val name: String,
    val description: String = "",

    @Enumerated(EnumType.STRING)
    val type: TaskType,

    @Enumerated(EnumType.STRING)
    val frequency: TaskFrequency,

    /** Default assignee (pre-populated); can be overridden per period */
    @Enumerated(EnumType.STRING)
    val defaultAssignee: Assignee = Assignee.UNASSIGNED,

    val points: Int = 1,
    val timeWindow: String = "",
    val deadline: String = "",

    val active: Boolean = true,
    val sortOrder: Int = 0
)
