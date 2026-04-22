package com.amaral.hometask.service

import com.amaral.hometask.model.FamilyConfig
import com.amaral.hometask.model.dtos.FamilyConfigDto
import com.amaral.hometask.model.UpdateFamilyConfigRequest
import com.amaral.hometask.repository.FamilyConfigRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class FamilyConfigService(
    private val familyConfigRepo: FamilyConfigRepository
) {
    fun getFamilyConfig(): FamilyConfigDto {
        val cfg = familyConfigRepo.findById(1L).orElse(FamilyConfig())
        return FamilyConfigDto(cfg.child1Name, cfg.child2Name)
    }

    @Transactional
    fun updateFamilyConfig(req: UpdateFamilyConfigRequest): FamilyConfigDto {
        val cfg = FamilyConfig(id = 1L, child1Name = req.child1Name.trim(), child2Name = req.child2Name.trim())
        familyConfigRepo.save(cfg)
        return FamilyConfigDto(cfg.child1Name, cfg.child2Name)
    }
}

