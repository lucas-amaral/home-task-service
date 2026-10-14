package com.amaral.hometask.controller

import com.amaral.hometask.service.DailyRoutineResult
import com.amaral.hometask.service.DailyRoutineService
import com.amaral.hometask.service.ReminderResult
import com.amaral.hometask.service.ReminderService
import com.amaral.hometask.service.ReminderSlot
import com.amaral.hometask.util.DateTimeUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * The jobs of the day, for an external cron to call (the free hosting sleeps, so the internal
 * scheduler alone is not enough). Each call needs the header `X-Cron-Token` with the value of
 * CRON_TOKEN; while CRON_TOKEN is not set, these endpoints are off.
 */
@RestController
@RequestMapping("/api/cron")
class CronController(
    private val routine: DailyRoutineService,
    private val reminders: ReminderService,
    @Value("\${cron.token:}") private val token: String,
) {

    /** Closes yesterday (tasks not done become missed) and creates today's tasks. Safe to repeat. */
    @PostMapping("/daily")
    fun daily(@RequestHeader("X-Cron-Token", required = false) given: String?): DailyRoutineResult {
        authorize(given)
        return routine.run(DateTimeUtils.today())
    }

    /** Sends the reminder of the slot (noon or evening) for today, once a day. */
    @PostMapping("/reminders")
    fun reminders(
        @RequestHeader("X-Cron-Token", required = false) given: String?,
        @RequestParam slot: ReminderSlot,
    ): ReminderResult {
        authorize(given)
        return reminders.send(slot, DateTimeUtils.today())
    }

    private fun authorize(given: String?) {
        if (token.isBlank()) throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CRON_TOKEN is not set")
        if (given != token) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid cron token")
    }
}
