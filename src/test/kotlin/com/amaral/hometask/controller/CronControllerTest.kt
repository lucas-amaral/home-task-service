package com.amaral.hometask.controller

import com.amaral.hometask.service.DailyRoutineResult
import com.amaral.hometask.service.DailyRoutineService
import com.amaral.hometask.service.ReminderResult
import com.amaral.hometask.service.ReminderService
import com.amaral.hometask.service.ReminderSlot
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDate

@WebMvcTest(CronController::class)
@ActiveProfiles("test")
@TestPropertySource(properties = ["cron.token=secret"])
class CronControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockBean lateinit var routine: DailyRoutineService
    @MockBean lateinit var reminders: ReminderService

    @Test
    fun `the daily routine runs with the right token`() {
        val today = LocalDate.now()
        whenever(routine.run(today)).thenReturn(DailyRoutineResult(today.minusDays(1), 2, today, 8))

        mvc.post("/api/cron/daily") { header("X-Cron-Token", "secret") }.andExpect {
            status { isOk() }
            jsonPath("$.missedMarked") { value(2) }
            jsonPath("$.assignmentsReady") { value(8) }
        }
    }

    @Test
    fun `without the token, or with a wrong one, nothing runs`() {
        mvc.post("/api/cron/daily").andExpect { status { isUnauthorized() } }
        mvc.post("/api/cron/daily") { header("X-Cron-Token", "guess") }.andExpect { status { isUnauthorized() } }
        mvc.post("/api/cron/reminders?slot=NOON").andExpect { status { isUnauthorized() } }

        verify(routine, never()).run(any())
        verify(reminders, never()).send(any(), any())
    }

    @Test
    fun `a reminder slot is sent with the right token`() {
        val today = LocalDate.now()
        whenever(reminders.send(ReminderSlot.EVENING, today)).thenReturn(ReminderResult(today, ReminderSlot.EVENING, false, mapOf("Clara" to 1)))

        mvc.post("/api/cron/reminders?slot=EVENING") { header("X-Cron-Token", "secret") }.andExpect {
            status { isOk() }
            jsonPath("$.notified.Clara") { value(1) }
        }
    }
}
