package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.repository.AssignmentRepository
import com.amaral.hometask.repository.PointLedgerRepository
import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeleteAssignmentTest {

    private val assignmentRepo: AssignmentRepository = mock()
    private val taskRepo: TaskRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val familyConfigService: FamilyConfigService = mock()

    private val service = AssignmentService(assignmentRepo, taskRepo, ledgerRepo, familyConfigService)

    private val monday = LocalDate.of(2024, 6, 10)

    private fun regularTask(id: Long = 1L) = Task(
        id = id, name = "Aspirar", type = TaskType.DAILY,
        frequency = TaskFrequency.DAILY, defaultAssignee = Assignee.CHILD1,
        points = 2, oneOff = false
    )

    @BeforeEach
    fun setup() {
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `deleteAssignment removes row`() {
        val task = regularTask()
        val assignment = Assignment(id = 10L, task = task, assignedTo = Assignee.CHILD1, periodDate = monday)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))

        service.deleteAssignment(10L)

        verify(assignmentRepo).deleteById(10L)
    }

    @Test
    fun `deleteAssignment reverses points when completed`() {
        val task = regularTask()
        val assignment = Assignment(
            id = 10L, task = task, assignedTo = Assignee.CHILD2,
            periodDate = monday, completedAt = LocalDateTime.now(), bonusEarned = true
        )
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))

        service.deleteAssignment(10L)

        // task.points(2) + bonus(1) = 3 reversed
        verify(ledgerRepo).save(argThat { delta == -3 && assignee == Assignee.CHILD2 })
        verify(assignmentRepo).deleteById(10L)
    }

    @Test
    fun `deleteAssignment does not reverse points when not completed`() {
        val task = regularTask()
        val assignment = Assignment(id = 10L, task = task, assignedTo = Assignee.CHILD1, periodDate = monday)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))

        service.deleteAssignment(10L)

        verify(ledgerRepo, never()).save(any())
        verify(assignmentRepo).deleteById(10L)
    }

    @Test
    fun `deleteAssignment reverses points for BOTH children when joint`() {
        val task = regularTask()
        val assignment = Assignment(
            id = 10L, task = task, assignedTo = Assignee.BOTH,
            periodDate = monday, completedAt = LocalDateTime.now()
        )
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))

        service.deleteAssignment(10L)

        verify(ledgerRepo).save(argThat { delta == -2 && assignee == Assignee.CHILD1 })
        verify(ledgerRepo).save(argThat { delta == -2 && assignee == Assignee.CHILD2 })
    }

    @Test
    fun `deleteAssignment throws when assignment not found`() {
        whenever(assignmentRepo.findById(999L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.deleteAssignment(999L) }
        verify(assignmentRepo, never()).deleteById(any())
    }
}
