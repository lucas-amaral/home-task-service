package com.amaral.hometask.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object DateTimeUtils {

    fun today(): LocalDate = LocalDate.now()

    fun weekStart(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

