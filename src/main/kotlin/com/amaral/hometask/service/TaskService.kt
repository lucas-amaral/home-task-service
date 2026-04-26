package com.amaral.hometask.service

import com.amaral.hometask.model.requests.CreateTaskRequest
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.UpdateTaskRequest
import com.amaral.hometask.model.dtos.TaskDto
import com.amaral.hometask.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskService(private val taskRepo: TaskRepository) {

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
                deadline = req.deadline, sortOrder = req.sortOrder,
                oneOff = req.oneOff
            )
        ).toDto()

    /**
     * Update an existing task.
     * Only the fields in [UpdateTaskRequest] are changed; the id and oneOff flag
     * are preserved from the existing row.
     */
    @Transactional
    fun updateTask(id: Long, req: UpdateTaskRequest): TaskDto {
        val existing = taskRepo.findById(id)
            .orElseThrow { NoSuchElementException("Task $id not found") }
        return taskRepo.save(
            existing.copy(
                name = req.name, description = req.description,
                type = req.type, frequency = req.frequency,
                defaultAssignee = req.defaultAssignee,
                points = req.points, timeWindow = req.timeWindow,
                deadline = req.deadline, sortOrder = req.sortOrder,
                active = req.active
            )
        ).toDto()
    }

    /**
     * Soft-delete: sets active=false so the task no longer appears on the board
     * or in future assignment creation. Existing assignment rows are kept intact
     * for historical reporting.
     */
    @Transactional
    fun deleteTask(id: Long) {
        val existing = taskRepo.findById(id)
            .orElseThrow { NoSuchElementException("Task $id not found") }
        taskRepo.save(existing.copy(active = false))
    }

    private fun Task.toDto() = TaskDto(
        id, name, description, type, frequency, defaultAssignee,
        points, timeWindow, deadline, active, sortOrder, oneOff
    )
}
