package com.amaral.hometask.controller;

import com.amaral.hometask.service.WeekSummaryService
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/weeks")
class WeekSummaryController(private val service: WeekSummaryService) {

    @GetMapping("/{weekStart}")
    fun getWeekSummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)weekStart:LocalDate
    ) = service.getWeekSummary(weekStart)
}
