package com.amaral.hometask.service

import com.amaral.hometask.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Feature 3 — Hourly deadline notification scheduler.
 *
 * Fires at minute 5 of every hour (01:05, 02:05, … 23:05) and checks whether
 * any assignment's deadline has passed in the current hour without being
 * completed. When it finds one it calls [WhatsAppNotificationService] to send
 * a reminder message.
 *
 * Why minute 5?
 *   The deadline string stored on a Task is just an hour boundary (e.g. "13:05").
 *   We fire 5 minutes into each hour so the deadline is unambiguously past
 *   even accounting for small clock skew between the app and Railway's scheduler.
 *
 * Cron expression: "0 5 * * * *"
 *   second=0, minute=5, every hour, every day, every month, every weekday.
 *
 * Set SCHEDULER_TIMEZONE (e.g. "America/Sao_Paulo") in Railway env vars so
 * the hour comparison is done in the family's local time, not UTC.
 */
@Component
class DeadlineNotificationScheduler(
    private val assignmentService: AssignmentService,
    private val notificationService: WhatsAppNotificationService,
    @Value("\${scheduler.timezone:UTC}") private val timezone: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scheduler.notification-cron:0 5 * * * *}", zone = "\${scheduler.timezone:UTC}")
    fun checkHourlyDeadlines() {
        val now  = java.time.ZonedDateTime.now(java.time.ZoneId.of(timezone))
        val date = now.toLocalDate()
        val hour = now.hour

        log.debug("Hourly deadline check at {}:{} ({})", hour, "05", timezone)

        try {
            val overdue = assignmentService.findOverdueForNotification(date, hour)
            if (overdue.isEmpty()) {
                log.debug("No overdue assignments at hour {}", hour)
                return
            }

            log.info("Found {} overdue assignment(s) at hour {} — sending notifications", overdue.size, hour)
            overdue.forEach { assignment ->
                try {
                    notificationService.sendDeadlineMissed(assignment)
                } catch (ex: Exception) {
                    log.error(
                        "Failed to send notification for assignment {} ({}): {}",
                        assignment.id, assignment.task.name, ex.message
                    )
                }
            }
        } catch (ex: Exception) {
            log.error("Hourly deadline check failed at hour {}: {}", hour, ex.message, ex)
        }
    }
}
