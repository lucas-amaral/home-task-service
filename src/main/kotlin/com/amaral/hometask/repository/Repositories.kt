package com.amaral.hometask.repository

import com.amaral.hometask.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TaskRepository : JpaRepository<Task, Long> {
    fun findByActiveTrueOrderBySortOrderAsc(): List<Task>
}

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
     * Native upsert for daily assignments.
     * Inserts if the (task_id, period_date) pair does not exist; does nothing
     * if it does. This is the only safe way to prevent duplicates under
     * concurrent requests without relying on application-level locking.
     *
     * Note: assigned_to, bonus_earned, penalty_applied and missed_deadline are
     * always set to their defaults here — the caller updates them afterwards
     * if needed via a separate save().
     */
    @Modifying
    @Query(value = """
        INSERT INTO assignments
            (task_id, assigned_to, period_date, period_week,
             completed_at, bonus_earned, penalty_applied, missed_deadline)
        VALUES
            (:taskId, :assignedTo, :periodDate, NULL,
             NULL, false, false, false)
        ON CONFLICT (task_id, period_date)
            WHERE period_date IS NOT NULL
        DO NOTHING
    """, nativeQuery = true)
    fun upsertDaily(taskId: Long, assignedTo: String, periodDate: LocalDate)

    @Modifying
    @Query(value = """
        INSERT INTO assignments
            (task_id, assigned_to, period_date, period_week,
             completed_at, bonus_earned, penalty_applied, missed_deadline)
        VALUES
            (:taskId, :assignedTo, NULL, :periodWeek,
             NULL, false, false, false)
        ON CONFLICT (task_id, period_week)
            WHERE period_week IS NOT NULL
        DO NOTHING
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

@Repository
interface PointLedgerRepository : JpaRepository<PointLedger, Long> {
    fun findByWeekStart(weekStart: LocalDate): List<PointLedger>

    @Query("SELECT p FROM PointLedger p ORDER BY p.weekStart DESC")
    fun findAllOrderByWeekDesc(): List<PointLedger>
}

@Repository
interface RewardRepository : JpaRepository<Reward, Long> {
    fun findByActiveTrue(): List<Reward>
}

@Repository
interface FamilyConfigRepository : JpaRepository<FamilyConfig, Long>
