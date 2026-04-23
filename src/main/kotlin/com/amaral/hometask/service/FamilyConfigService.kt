package com.amaral.hometask.service

import com.amaral.hometask.model.FamilyConfig
import com.amaral.hometask.model.dtos.FamilyConfigDto
import com.amaral.hometask.model.requests.UpdateFamilyConfigRequest
import com.amaral.hometask.repository.FamilyConfigRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FamilyConfigService(
    private val familyConfigRepo: FamilyConfigRepository
) {
    fun getFamilyConfig(): FamilyConfigDto {
        val cfg = familyConfigRepo.findById(1L).orElse(FamilyConfig())
        return cfg.toDto()
    }

    fun getFamilyConfigEntity(): FamilyConfig =
        familyConfigRepo.findById(1L).orElse(FamilyConfig())

    @Transactional
    fun updateFamilyConfig(req: UpdateFamilyConfigRequest): FamilyConfigDto {
        val cfg = FamilyConfig(
            id = 1L,
            child1Name = req.child1Name.trim(),
            child2Name = req.child2Name.trim(),
            child1Phone = req.child1Phone.trim(),
            child2Phone = req.child2Phone.trim()
        )
        familyConfigRepo.save(cfg)
        return cfg.toDto()
    }

    private fun FamilyConfig.toDto() = FamilyConfigDto(
        child1Name = child1Name,
        child2Name = child2Name,
        child1Phone = child1Phone,
        child2Phone = child2Phone
    )
}
