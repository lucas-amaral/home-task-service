package com.amaral.hometask.controller

import com.amaral.hometask.service.PointLedgerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/points")
class PointLedgerController(private val service: PointLedgerService) {

    @GetMapping("/history")
    fun pointsHistory() = service.getPointsHistory()
}

