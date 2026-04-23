package com.amaral.hometask.controller

import com.amaral.hometask.model.requests.UpdateFamilyConfigRequest
import com.amaral.hometask.service.FamilyConfigService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/config")
class FamilyConfigController(private val service: FamilyConfigService) {

    @GetMapping
    fun getConfig() = service.getFamilyConfig()

    @PutMapping
    fun updateConfig(@RequestBody req: UpdateFamilyConfigRequest) = service.updateFamilyConfig(req)
}

