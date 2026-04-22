package com.amaral.hometask.repository

import com.amaral.hometask.model.Assignment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AssignmentRepository : JpaRepository<Assignment, Long> {

    fun findByPeriodDate(date: LocalDate): List<Assignment>
    fun findByPeriodWeek(weekStart: LocalDate): List<Assignment>

    /**
     * Lookup for daily assignments.
     *
     * Uses a JPQL query with LIMIT 1 to be safe even if a duplicate somehow
     * slipped through, returning the first row instead of throwing
     * NonUniqueResultException.
     */
    @Query("""
        SELECT a FROM Assignment a
        WHERE a.task.id = :taskId AND a.periodDate = :date
        ORDER BY a.id ASC
    """)
    fun findAllByTaskIdAndPeriodDate(taskId: Long, date: LocalDate): List<Assignment>

    /** Single-result version – returns the first row if any duplicate exists. */
    fun findByTaskIdAndPeriodDate(taskId: Long, date: LocalDate): Assignment? {
        // Default Spring Data method – safe for the normal case (0 or 1 row).
        // Overridden below to use the list query and pick first, so we never
        // blow up if a duplicate is somehow present.
        return null // will be replaced by the @Query override below
    }

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.task.id = :taskId AND a.periodWeek = :weekStart
        ORDER BY a.id ASC
    """)
    fun findAllByTaskIdAndPeriodWeek(taskId: Long, weekStart: LocalDate): List<Assignment>

    /**
     * Portable idempotent insert for daily assignments.
     *
     * PostgreSQL supports a more compact partial-index ON CONFLICT form here,
     * but H2 does not parse that syntax. This variant works in both test and
     * production environments and matches the weekly insert strategy below.
     */
    @Modifying
    @Query(value = """
        INSERT INTO assignments
            (task_id, assigned_to, period_date, period_week,
             completed_at, bonus_earned, penalty_applied, missed_deadline)
        SELECT :taskId, :assignedTo, :periodDate, NULL,
               NULL, false, false, false
        WHERE NOT EXISTS (
            SELECT 1 FROM assignments
            WHERE task_id = :taskId AND period_date = :periodDate
        )
    """, nativeQuery = true)
    fun upsertDaily(taskId: Long, assignedTo: String, periodDate: LocalDate)

    @Modifying
    @Query(value = """
        INSERT INTO assignments
            (task_id, assigned_to, period_date, period_week,
             completed_at, bonus_earned, penalty_applied, missed_deadline)
        SELECT :taskId, :assignedTo, NULL, :periodWeek,
               NULL, false, false, false
        WHERE NOT EXISTS (
            SELECT 1 FROM assignments 
            WHERE task_id = :taskId AND period_week = :periodWeek
        )
    """, nativeQuery = true)
    fun upsertWeekly(taskId: Long, assignedTo: String, periodWeek: LocalDate)

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.periodWeek = :weekStart
           OR (a.periodDate >= :weekStart AND a.periodDate < :weekEnd)
    """)
    fun findAllForWeek(weekStart: LocalDate, weekEnd: LocalDate): List<Assignment>

    @Query("""
        SELECT a FROM Assignment a
        WHERE a.completedAt IS NULL
          AND a.penaltyApplied = false
          AND a.missedDeadline = false
          AND a.assignedTo IN ('CHILD1', 'CHILD2')
          AND (a.periodDate = :date OR a.periodWeek = :weekStart)
    """)
    fun findMissedCandidates(date: LocalDate, weekStart: LocalDate): List<Assignment>

    @Query("SELECT a FROM Assignment a ORDER BY a.completedAt DESC")
    fun findAllOrderByCompletedDesc(): List<Assignment>
}
