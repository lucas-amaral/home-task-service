package com.amaral.hometask.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One row per reminder already sent (date + slot), so the internal scheduler and the external
 * cron can both try and the children still get each reminder only once.
 */
@Entity
@Table(name = "reminder_log", uniqueConstraints = [UniqueConstraint(columnNames = ["reminder_date", "slot"])])
data class ReminderLog(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @jakarta.persistence.Column(name = "reminder_date")
    val date: LocalDate,
    val slot: String,
    val sentAt: LocalDateTime = LocalDateTime.now(),
)
