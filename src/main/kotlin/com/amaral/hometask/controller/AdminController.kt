package com.amaral.hometask.controller

import com.amaral.hometask.service.AssignmentService
import com.amaral.hometask.service.FamilyConfigService
import com.amaral.hometask.util.DateTimeUtils
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val familyConfigService: FamilyConfigService,
    private val assignmentService: AssignmentService
) {

    @PostMapping("/run-deadline-check")
    fun runDeadlineCheck(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): Map<String, Any> {
        val target = date ?: DateTimeUtils.today()
        val count = assignmentService.applyMissedDeadlinePenalties(target)
        return mapOf("date" to target, "penaltiesApplied" to count)
    }
}