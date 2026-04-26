package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.*

class TaskServiceTest {

    private val taskRepo: TaskRepository = mock()
    private val service = TaskService(taskRepo)

    private fun makeTask(id: Long = 1L, active: Boolean = true) = Task(
        id = id, name = "Aspirar a sala",
        type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
        defaultAssignee = Assignee.CHILD1,
        points = 1, active = active
    )

    @Test
    fun `updateTask changes all editable fields`() {
        val existing = makeTask()
        val req = UpdateTaskRequest(
            name = "Aspirar quartos", description = "Quarto e corredor",
            type = TaskType.WEEKLY, frequency = TaskFrequency.WEEKLY,
            defaultAssignee = Assignee.CHILD2, points = 3,
            timeWindow = "08:00 – 09:00", deadline = "09:00",
            sortOrder = 5, active = true
        )
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(existing))
        whenever(taskRepo.save(any<Task>())).thenAnswer { it.arguments[0] }

        val result = service.updateTask(1L, req)

        verify(taskRepo).save(argThat {
            name == "Aspirar quartos" &&
            type == TaskType.WEEKLY &&
            points == 3 &&
            defaultAssignee == Assignee.CHILD2
        })
        assertEquals("Aspirar quartos", result.name)
        assertEquals(TaskType.WEEKLY, result.type)
        assertEquals(3, result.points)
    }

    @Test
    fun `updateTask preserves oneOff flag`() {
        val existing = makeTask().copy(oneOff = true)
        val req = UpdateTaskRequest(
            name = "Tarefa avulsa editada",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            active = true
        )
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(existing))
        whenever(taskRepo.save(any<Task>())).thenAnswer { it.arguments[0] }

        service.updateTask(1L, req)

        verify(taskRepo).save(argThat { oneOff })
    }

    @Test
    fun `updateTask throws when task not found`() {
        whenever(taskRepo.findById(999L)).thenReturn(Optional.empty())
        val req = UpdateTaskRequest(
            name = "X", type = TaskType.DAILY, frequency = TaskFrequency.DAILY
        )
        assertThrows<NoSuchElementException> { service.updateTask(999L, req) }
        verify(taskRepo, never()).save(any())
    }

    @Test
    fun `deleteTask sets active=false (soft delete)`() {
        val existing = makeTask(active = true)
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(existing))
        whenever(taskRepo.save(any<Task>())).thenAnswer { it.arguments[0] }

        service.deleteTask(1L)

        verify(taskRepo).save(argThat { !active })
    }

    @Test
    fun `deleteTask throws when task not found`() {
        whenever(taskRepo.findById(999L)).thenReturn(Optional.empty())
        assertThrows<NoSuchElementException> { service.deleteTask(999L) }
        verify(taskRepo, never()).save(any())
    }

    @Test
    fun `listTasks only returns active tasks`() {
        val active   = makeTask(id = 1L, active = true)
        val inactive = makeTask(id = 2L, active = false)
        whenever(taskRepo.findByActiveTrueOrderBySortOrderAsc()).thenReturn(listOf(active))

        val result = service.listTasks()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }
}
