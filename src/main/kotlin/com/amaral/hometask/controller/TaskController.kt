package com.amaral.hometask.controller

import com.amaral.hometask.model.requests.CreateTaskRequest
import com.amaral.hometask.model.UpdateTaskRequest
import com.amaral.hometask.service.TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val service: TaskService) {

    @GetMapping
    fun listTasks() = service.listTasks()

    @PostMapping
    fun createTask(@RequestBody req: CreateTaskRequest) = service.createTask(req)

    /** Update an existing task's fields. */
    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: Long,
        @RequestBody req: UpdateTaskRequest
    ) = service.updateTask(id, req)

    /**
     * Soft-delete: marks the task as inactive.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: Long): ResponseEntity<Void> {
        service.deleteTask(id)
        return ResponseEntity.noContent().build()
    }
}
