package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.repository.AssignmentRepository
import com.amaral.hometask.repository.PointLedgerRepository
import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.*

class OneOffTaskTest {

    private val taskRepo: TaskRepository = mock()
    private val assignmentRepo: AssignmentRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val familyConfigService: FamilyConfigService = mock()

    private val service = AssignmentService(assignmentRepo, taskRepo, ledgerRepo, familyConfigService)

    private val today = LocalDate.of(2024, 6, 10) // a Monday

    private fun makeOneOffTask(id: Long = 99L) = Task(
        id = id, name = "Regar plantas",
        type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
        defaultAssignee = Assignee.CHILD1, points = 1,
        oneOff = true
    )

    private fun makeAssignment(
        id: Long = 10L,
        task: Task = makeOneOffTask(),
        completed: Boolean = false
    ) = Assignment(
        id = id, task = task,
        assignedTo = Assignee.CHILD1,
        periodDate = today,
        completedAt = if (completed) java.time.LocalDateTime.now() else null
    )

    @BeforeEach
    fun setup() {
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `deleteAssignment on one-off deactivates parent task`() {
        val task = makeOneOffTask()
        val assignment = makeAssignment(task = task)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(taskRepo.save(any<Task>())).thenReturn(task.copy(active = false))

        service.deleteAssignment(10L)

        // Task should be deactivated
        verify(taskRepo).save(argThat { !active && oneOff })
        verify(assignmentRepo).deleteById(10L)
    }

    @Test
    fun `deleteAssignment on one-off reverses points when completed`() {
        val task = makeOneOffTask(id = 99L)
        val assignment = makeAssignment(task = task, completed = true)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(taskRepo.save(any<Task>())).thenReturn(task.copy(active = false))
        whenever(ledgerRepo.findByWeekStart(any())).thenReturn(emptyList())

        service.deleteAssignment(10L)

        // Points should be reversed (delta = -1)
        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD1 })
        verify(assignmentRepo).deleteById(10L)
    }

    @Test
    fun `deleteAssignment on regular assignment does NOT deactivate task`() {
        val regularTask = Task(
            id = 1L, name = "Aspirar", type = TaskType.DAILY,
            frequency = TaskFrequency.DAILY, defaultAssignee = Assignee.CHILD1,
            points = 1, oneOff = false
        )
        val assignment = makeAssignment(task = regularTask)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))

        service.deleteAssignment(10L)

        verify(taskRepo, never()).save(any())
        verify(assignmentRepo).save(argThat {
            id == 10L &&
            deleted == true &&
            completedAt == null &&
            !bonusEarned &&
            !penaltyApplied &&
            !missedDeadline
        })
        verify(assignmentRepo, never()).deleteById(any())
    }
}
