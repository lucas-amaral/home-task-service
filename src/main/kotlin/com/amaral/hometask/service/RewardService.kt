package com.amaral.hometask.service

import com.amaral.hometask.model.dtos.RewardDto
import com.amaral.hometask.repository.RewardRepository
import org.springframework.stereotype.Service

@Service
class RewardService(
    private val rewardRepo: RewardRepository
) {

    fun listRewards(): List<RewardDto> =
        rewardRepo.findByActiveTrue().map { RewardDto(it.id, it.name, it.pointsCost, it.emoji) }
}

