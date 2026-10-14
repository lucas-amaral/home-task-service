package com.amaral.hometask.service

import com.amaral.hometask.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * The noon and evening reminders, while the service is awake. When it was asleep, [WakeUpCatchUp]
 * sends the one due as soon as it starts; each one goes out only once a day either way.
 */
@Component
@Lazy(false)
class ReminderScheduler(private val reminders: ReminderService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scheduler.reminder-noon-cron:0 0 12 * * *}", zone = "\${scheduler.timezone:UTC}")
    fun noon() = run(ReminderSlot.NOON)

    @Scheduled(cron = "\${scheduler.reminder-evening-cron:0 0 19 * * *}", zone = "\${scheduler.timezone:UTC}")
    fun evening() = run(ReminderSlot.EVENING)

    private fun run(slot: ReminderSlot) {
        try {
            reminders.send(slot, DateTimeUtils.today())
        } catch (ex: Exception) {
            log.error("Reminder {} failed: {}", slot, ex.message, ex)
        }
    }
}
