package com.amaral.hometask.controller

import com.amaral.hometask.model.AssignRequest
import com.amaral.hometask.model.AssignmentDto
import com.amaral.hometask.model.CompleteRequest
import com.amaral.hometask.model.CreateTaskRequest
import com.amaral.hometask.model.UpdateFamilyConfigRequest
import com.amaral.hometask.service.HomeTaskService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class HomeTaskController(private val service: HomeTaskService) {

    @GetMapping("/health")
    fun health() = mapOf("status" to "ok", "today" to service.today(), "weekStart" to service.weekStart())

    // ── Family config ────────────────────────────────────────────────────────

    @GetMapping("/config")
    fun getConfig() = service.getFamilyConfig()

    @PutMapping("/config")
    fun updateConfig(@RequestBody req: UpdateFamilyConfigRequest) = service.updateFamilyConfig(req)

    // ── Tasks ────────────────────────────────────────────────────────────────

    @GetMapping("/tasks")
    fun listTasks() = service.listTasks()

    @PostMapping("/tasks")
    fun createTask(@RequestBody req: CreateTaskRequest) = service.createTask(req)

    // ── Board ────────────────────────────────────────────────────────────────

    @GetMapping("/board")
    fun getBoard(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ) = service.getBoard(date ?: service.today())

    // ── Week summary ─────────────────────────────────────────────────────────

    @GetMapping("/weeks/{weekStart}")
    fun getWeekSummary(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStart: LocalDate
    ) = service.getWeekSummary(weekStart)

    // ── Assignments ──────────────────────────────────────────────────────────

    @PostMapping("/assignments/assign")
    fun assign(@RequestBody req: AssignRequest): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.assignTask(req))

    @PostMapping("/assignments/{id}/complete")
    fun complete(
        @PathVariable id: Long,
        @RequestBody req: CompleteRequest
    ): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.completeAssignment(id, req))

    @PostMapping("/assignments/{id}/uncomplete")
    fun uncomplete(@PathVariable id: Long): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.uncompleteAssignment(id))

    @PostMapping("/assignments/{id}/penalty")
    fun penalty(@PathVariable id: Long): ResponseEntity<AssignmentDto> =
        ResponseEntity.ok(service.applyPenalty(id))

    // ── Points history ───────────────────────────────────────────────────────

    @GetMapping("/points/history")
    fun pointsHistory() = service.getPointsHistory()

    // ── Rewards ──────────────────────────────────────────────────────────────

    @GetMapping("/rewards")
    fun rewards() = service.listRewards()
}
