package com.amaral.hometask.service

import com.amaral.hometask.model.requests.CreateTaskRequest
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.dtos.TaskDto
import com.amaral.hometask.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskService(
    private val taskRepo: TaskRepository
) {

    fun listTasks(): List<TaskDto> =
        taskRepo.findByActiveTrueOrderBySortOrderAsc().map { it.toDto() }

    @Transactional
    fun createTask(req: CreateTaskRequest): TaskDto =
        taskRepo.save(
            Task(
                name = req.name, description = req.description,
                type = req.type, frequency = req.frequency,
                defaultAssignee = req.defaultAssignee,
                points = req.points, timeWindow = req.timeWindow,
                deadline = req.deadline, sortOrder = req.sortOrder
            )
        ).toDto()

    // ── Mappers ──────────────────────────────────────────────────────

    private fun Task.toDto() = TaskDto(
        id, name, description, type, frequency, defaultAssignee,
        points, timeWindow, deadline, deadlineDate, active, sortOrder
    )
}

