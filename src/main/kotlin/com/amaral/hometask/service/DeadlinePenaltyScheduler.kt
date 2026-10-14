package com.amaral.hometask.service

import com.amaral.hometask.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Runs the daily routine just after midnight: the day that ended is closed (tasks not done are
 * marked as missed, with their −1) and the new day gets its tasks.
 *
 * Cron expression: "0 1 0 * * *" = 00:01 every day, in scheduler.timezone.
 *
 * Render's free plan is usually asleep at that hour, so [WakeUpCatchUp] runs the same routine
 * whenever the service starts. Running it twice changes nothing.
 */
@Component
@Lazy(false)
class DeadlinePenaltyScheduler(private val routine: DailyRoutineService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scheduler.penalty-cron:0 1 0 * * *}", zone = "\${scheduler.timezone:UTC}")
    fun applyDailyPenalties() {
        try {
            routine.run(DateTimeUtils.today())
        } catch (ex: Exception) {
            log.error("Daily routine failed: {}", ex.message, ex)
        }
    }
}
