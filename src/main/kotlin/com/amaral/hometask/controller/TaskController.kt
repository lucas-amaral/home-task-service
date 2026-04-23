package com.amaral.hometask.controller

import com.amaral.hometask.model.requests.CreateTaskRequest
import com.amaral.hometask.service.TaskService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val service: TaskService) {

    @GetMapping
    fun listTasks() = service.listTasks()

    @PostMapping
    fun createTask(@RequestBody req: CreateTaskRequest) = service.createTask(req)
}

