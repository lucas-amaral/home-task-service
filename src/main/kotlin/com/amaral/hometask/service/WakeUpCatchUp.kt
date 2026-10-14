package com.amaral.hometask.service

import com.amaral.hometask.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Render's free plan puts the service to sleep, and the schedulers only fire while it is awake. So
 * every time it starts it catches up on its own: closes yesterday, creates today's tasks and sends the
 * reminder of the current hour if it has not gone out yet. Both steps are safe to repeat.
 *
 * Anything that wakes the service at noon and at 19:00 (a request to GET /api/health from an
 * external scheduler) is then enough for the reminders to go out on time.
 */
@Component
@Lazy(false)
class WakeUpCatchUp(
    private val routine: DailyRoutineService,
    private val reminders: ReminderService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() = catchUp(LocalDateTime.now())

    fun catchUp(now: LocalDateTime) {
        val today = now.toLocalDate()
        try {
            routine.run(today)
        } catch (ex: Exception) {
            log.error("Daily routine on wake-up failed: {}", ex.message, ex)
        }
        val slot = ReminderSlot.dueAt(now.toLocalTime()) ?: return
        try {
            reminders.send(slot, today)
        } catch (ex: Exception) {
            log.error("Reminder {} on wake-up failed: {}", slot, ex.message, ex)
        }
    }
}
