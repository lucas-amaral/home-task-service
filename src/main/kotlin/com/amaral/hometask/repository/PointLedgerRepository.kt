package com.amaral.hometask.repository

import com.amaral.hometask.model.PointLedger
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PointLedgerRepository : JpaRepository<PointLedger, Long> {
    fun findByWeekStart(weekStart: LocalDate): List<PointLedger>

    @Query("SELECT p FROM PointLedger p ORDER BY p.weekStart DESC")
    fun findAllOrderByWeekDesc(): List<PointLedger>
}
