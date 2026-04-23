package com.amaral.hometask.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

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

    /** Legacy text deadline kept for display ("até 19:30") */
    val deadline: String = "",

    /**
     * Optional hard deadline date-time for the assignment.
     * When set and the assignment is not completed by this instant,
     * a WhatsApp notification is sent to the child's registered phone.
     */
    val deadlineDate: LocalDateTime? = null,

    val active: Boolean = true,
    val sortOrder: Int = 0
)
