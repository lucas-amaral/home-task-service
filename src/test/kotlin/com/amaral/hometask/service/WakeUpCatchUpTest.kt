package com.amaral.hometask.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WakeUpCatchUpTest {

    private val routine: DailyRoutineService = mock()
    private val reminders: ReminderService = mock()
    private val catchUp = WakeUpCatchUp(routine, reminders)
    private val day = LocalDate.of(2026, 10, 14)

    @Test
    fun `closes yesterday and prepares today whenever it wakes up`() {
        catchUp.catchUp(day.atTime(7, 30))

        verify(routine).run(day)
        verify(reminders, never()).send(any(), any())
    }

    @Test
    fun `sends the noon reminder when woken at noon`() {
        catchUp.catchUp(day.atTime(12, 1))

        verify(reminders).send(ReminderSlot.NOON, day)
    }

    @Test
    fun `sends the evening reminder when woken in the evening`() {
        catchUp.catchUp(day.atTime(19, 2))

        verify(reminders).send(ReminderSlot.EVENING, day)
    }

    @Test
    fun `still sends the reminder when the routine fails`() {
        whenever(routine.run(day)).thenThrow(IllegalStateException("db down"))

        catchUp.catchUp(LocalDateTime.of(day, LocalTime.NOON))

        verify(reminders).send(ReminderSlot.NOON, day)
    }

    @Test
    fun `each reminder has its window and none goes out at night`() {
        assertNull(ReminderSlot.dueAt(LocalTime.of(11, 59)))
        assertEquals(ReminderSlot.NOON, ReminderSlot.dueAt(LocalTime.NOON))
        assertEquals(ReminderSlot.NOON, ReminderSlot.dueAt(LocalTime.of(18, 59)))
        assertEquals(ReminderSlot.EVENING, ReminderSlot.dueAt(LocalTime.of(19, 0)))
        assertNull(ReminderSlot.dueAt(LocalTime.of(22, 0)))
    }
}
