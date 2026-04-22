package com.amaral.hometask.repository

import com.amaral.hometask.model.Reward
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RewardRepository : JpaRepository<Reward, Long> {
    fun findByActiveTrue(): List<Reward>
}
