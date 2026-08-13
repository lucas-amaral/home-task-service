package com.amaral.hometask.controller

import com.amaral.hometask.model.dtos.WeeklyStatusDto
import com.amaral.hometask.service.FamilyConfigService
import com.amaral.hometask.service.PointLedgerService
import com.amaral.hometask.util.DateTimeUtils
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/points")
class PointLedgerController(
    private val service: PointLedgerService,
    private val familyConfigService: FamilyConfigService
) {

    @GetMapping("/history")
    fun pointsHistory() = service.getPointsHistory()

    /** Occurrence count + escalating consequence ladder for a given week (defaults to current). */
    @GetMapping("/status")
    fun weeklyStatus(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        weekStart: LocalDate?
    ): List<WeeklyStatusDto> {
        val cfg = familyConfigService.getFamilyConfig()
        return service.weeklyStatus(weekStart ?: DateTimeUtils.weekStart(), cfg.child1Name, cfg.child2Name)
    }
}
