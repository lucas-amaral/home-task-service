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

    @Enumerated(EnumType.STRING)
    val defaultAssignee: Assignee = Assignee.UNASSIGNED,

    val points: Int = 1,
    val timeWindow: String = "",

    /**
     * Deadline as HH:mm string (e.g. "13:05").
     * Parsed by DeadlineNotificationScheduler to determine if a task is overdue.
     */
    val deadline: String = "",

    val active: Boolean = true,
    val sortOrder: Int = 0,

    /**
     * When true this task was created ad-hoc for a single day (one-off).
     * BoardService will NEVER call upsertDaily/upsertWeekly for one-off tasks —
     * the assignment created at the time of task creation is the only one ever made.
     */
    val oneOff: Boolean = false
)
