package com.amaral.hometask.service

import com.amaral.hometask.model.CreateTaskRequest
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TaskServiceTest {

    private val taskRepo: TaskRepository = mock()
    private val service = TaskService(taskRepo)

    @Test
    fun `createTask saves and returns task DTO`() {
        val req = CreateTaskRequest(
            name = "New Task",
            type = TaskType.DAILY,
            frequency = TaskFrequency.DAILY,
            points = 2
        )
        val saved = Task(
            id = 99L,
            name = "New Task",
            type = TaskType.DAILY,
            frequency = TaskFrequency.DAILY,
            points = 2
        )
        whenever(taskRepo.save(any<Task>())).thenReturn(saved)

        val dto = service.createTask(req)

        assertEquals(99L, dto.id)
        assertEquals("New Task", dto.name)
        verify(taskRepo).save(any())
    }
}
