package com.amaral.hometask.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateTimeUtilsTest {

    private val monday = LocalDate.of(2024, 1, 15)
    private val tuesday = monday.plusDays(1)

    @Test
    fun `weekStart returns Monday for any day of the week`() {
        assertEquals(monday, DateTimeUtils.weekStart(monday))
        assertEquals(monday, DateTimeUtils.weekStart(tuesday))
        assertEquals(monday, DateTimeUtils.weekStart(monday.plusDays(6)))
    }
}
