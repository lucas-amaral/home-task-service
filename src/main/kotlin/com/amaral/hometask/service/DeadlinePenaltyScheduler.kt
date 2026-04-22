package com.amaral.hometask.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Runs once per day, just after midnight, to apply −1 point penalties for
 * any assignments that were not completed before end-of-day.
 *
 * Cron expression: "0 1 0 * * *"
 *   = second=0, minute=1, hour=0 (00:01 every day)
 *
 * The 1-minute offset from midnight avoids any clock-skew edge cases with
 * "today" vs "yesterday" when the job fires.
 *
 * Railway runs containers in UTC. If the family is in a different timezone,
 * set the SCHEDULER_TIMEZONE env var (e.g. "America/Sao_Paulo") – it will
 * then fire at 00:01 in that timezone.
 */
@Component
class DeadlinePenaltyScheduler(private val service: AssignmentService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scheduler.penalty-cron:0 1 0 * * *}", zone = "\${scheduler.timezone:UTC}")
    fun applyDailyPenalties() {
        // We penalize for *yesterday* – the day that just ended
        val yesterday = LocalDate.now().minusDays(1)
        log.info("Running end-of-day penalty check for {}", yesterday)

        try {
            val count = service.applyMissedDeadlinePenalties(yesterday)
            if (count > 0) {
                log.info("Applied missed-deadline penalty to {} assignment(s) for {}", count, yesterday)
            } else {
                log.info("No missed deadlines for {}", yesterday)
            }
        } catch (ex: Exception) {
            log.error("Penalty scheduler failed for {}: {}", yesterday, ex.message, ex)
        }
    }
}
