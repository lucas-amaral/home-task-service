package com.amaral.hometask.repository

import com.amaral.hometask.model.ReminderLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ReminderLogRepository : JpaRepository<ReminderLog, Long> {
    fun existsByDateAndSlot(date: LocalDate, slot: String): Boolean
}
