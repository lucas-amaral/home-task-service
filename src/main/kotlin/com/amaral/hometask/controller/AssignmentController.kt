package com.amaral.hometask.controller

import com.amaral.hometask.model.AssignRequest
import com.amaral.hometask.model.dtos.AssignmentDto
import com.amaral.hometask.model.CompleteRequest
import com.amaral.hometask.service.AssignmentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/assignments")
class AssignmentController(private val service: AssignmentService) {

    @PostMapping("/assign")
    fun assign(@RequestBody req: AssignRequest): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.assignTask(req))

    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: Long,
        @RequestBody req: CompleteRequest
    ): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.completeAssignment(id, req))

    @PostMapping("/{id}/uncomplete")
    fun uncomplete(@PathVariable id: Long): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.uncompleteAssignment(id))

    @PostMapping("/{id}/penalty")
    fun penalty(@PathVariable id: Long): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.applyPenalty(id))

    @PostMapping("/{id}/unpenalty")
    fun unpenalty(@PathVariable id: Long): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.removeManualPenalty(id))
}

