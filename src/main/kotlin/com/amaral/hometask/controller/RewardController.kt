package com.amaral.hometask.controller

import com.amaral.hometask.service.RewardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/rewards")
class RewardController(private val service: RewardService) {

    @GetMapping
    fun listRewards() = service.listRewards()
}

