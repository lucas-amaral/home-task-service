package com.amaral.hometask.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One Assignment row per (task x period).
 * - DAILY tasks -> period = the specific date (weekStart = null, date = today)
 * - WEEKLY tasks -> period = week's Monday (weekStart = monday, date = null)
 *
 * This guarantees daily tasks reset every day and weekly tasks reset every week.
 */
@Entity
@Table(
    name = "assignments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["task_id", "period_date", "period_week"])]
)
data class Assignment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    val task: Task,

    @Enumerated(EnumType.STRING)
    val assignedTo: Assignee,

    /** Set for DAILY tasks - the exact calendar date */
    val periodDate: LocalDate? = null,

    /** Set for WEEKLY/BIWEEKLY/MONTHLY tasks - Monday of the relevant week */
    val periodWeek: LocalDate? = null,

    val completedAt: LocalDateTime? = null,
    val bonusEarned: Boolean = false,
    val penaltyApplied: Boolean = false
) {
    /** Convenience: the date to display on the post-it */
    val displayDate: LocalDate get() = periodDate ?: periodWeek!!
}
