package com.amaral.hometask.repository

import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.FamilyConfig
import com.amaral.hometask.model.PointLedger
import com.amaral.hometask.model.Reward
import com.amaral.hometask.model.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TaskRepository : JpaRepository<Task, Long> {
    fun findByActiveTrueOrderBySortOrderAsc(): List<Task>
}

@Repository
interface AssignmentRepository : JpaRepository<Assignment, Long> {

    /** All assignments for a specific calendar date (daily tasks) */
    fun findByPeriodDate(date: LocalDate): List<Assignment>

    /** All assignments for a week (weekly tasks) */
    fun findByPeriodWeek(weekStart: LocalDate): List<Assignment>

    /** Look up an existing daily assignment for a specific task+date */
    fun findByTaskIdAndPeriodDate(taskId: Long, date: LocalDate): Assignment?

    /** Look up an existing weekly assignment for a specific task+weekStart */
    fun findByTaskIdAndPeriodWeek(taskId: Long, weekStart: LocalDate): Assignment?

    /** All completed assignments in a date range (for history) */
    @Query("""
        SELECT a FROM Assignment a
        WHERE a.completedAt IS NOT NULL
        AND (a.periodDate BETWEEN :from AND :to
             OR a.periodWeek BETWEEN :from AND :to)
        ORDER BY a.completedAt DESC
    """)
    fun findCompletedBetween(from: LocalDate, to: LocalDate): List<Assignment>

    /** Weekly summary: all assignments (any period) within a week */
    @Query("""
        SELECT a FROM Assignment a
        WHERE a.periodWeek = :weekStart
           OR (a.periodDate >= :weekStart AND a.periodDate < :weekEnd)
    """)
    fun findAllForWeek(weekStart: LocalDate, weekEnd: LocalDate): List<Assignment>
}

@Repository
interface PointLedgerRepository : JpaRepository<PointLedger, Long> {
    fun findByWeekStart(weekStart: LocalDate): List<PointLedger>

    @Query("SELECT p FROM PointLedger p ORDER BY p.weekStart DESC")
    fun findAllOrderByWeekDesc(): List<PointLedger>

    @Query("""
        SELECT p.weekStart, p.assignee, SUM(p.delta)
        FROM PointLedger p
        WHERE p.weekStart BETWEEN :from AND :to
        GROUP BY p.weekStart, p.assignee
    """)
    fun sumByWeekAndAssigneeBetween(from: LocalDate, to: LocalDate): List<Array<Any>>
}

@Repository
interface RewardRepository : JpaRepository<Reward, Long> {
    fun findByActiveTrue(): List<Reward>
}

@Repository
interface FamilyConfigRepository : JpaRepository<FamilyConfig, Long>
