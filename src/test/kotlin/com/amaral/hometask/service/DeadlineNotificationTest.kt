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

class DeadlineNotificationTest {

    private val assignmentRepo: AssignmentRepository = mock()
    private val taskRepo: TaskRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val familyConfigService: FamilyConfigService = mock()

    private val service = AssignmentService(assignmentRepo, taskRepo, ledgerRepo, familyConfigService)

    private val monday = LocalDate.of(2024, 6, 10)

    private fun makeTask(deadline: String, id: Long = 1L) = Task(
        id = id, name = "Task $id",
        type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
        defaultAssignee = Assignee.CHILD1, points = 1,
        deadline = deadline
    )

    private fun makeAssignment(task: Task, id: Long = 10L) = Assignment(
        id = id, task = task,
        assignedTo = Assignee.CHILD1,
        periodDate = monday
    )

    @BeforeEach
    fun setup() {
        whenever(familyConfigService.getFamilyConfig()).thenReturn(
            com.amaral.hometask.model.dtos.FamilyConfigDto("C1", "C2")
        )
    }

    @Test
    fun `findOverdueForNotification returns assignment when deadline hour matches`() {
        val task = makeTask(deadline = "13:05")
        val assignment = makeAssignment(task)
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(assignment))

        val result = service.findOverdueForNotification(monday, hour = 13)

        assertEquals(1, result.size)
        assertEquals(assignment.id, result[0].id)
    }

    @Test
    fun `findOverdueForNotification ignores different hour`() {
        val task = makeTask(deadline = "13:05")
        val assignment = makeAssignment(task)
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(assignment))

        val result = service.findOverdueForNotification(monday, hour = 14)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findOverdueForNotification ignores blank deadline`() {
        val task = makeTask(deadline = "")
        val assignment = makeAssignment(task)
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(assignment))

        val result = service.findOverdueForNotification(monday, hour = 0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findOverdueForNotification ignores malformed deadline gracefully`() {
        val task = makeTask(deadline = "até 13h30")  // old free-text format
        val assignment = makeAssignment(task)
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(assignment))

        assertDoesNotThrow {
            val result = service.findOverdueForNotification(monday, hour = 13)
            assertTrue(result.isEmpty())  // malformed → skipped
        }
    }

    @Test
    fun `findOverdueForNotification returns multiple overdue assignments at same hour`() {
        val task1 = makeTask(deadline = "07:30", id = 1L)
        val task2 = makeTask(deadline = "07:05", id = 2L)
        val a1 = makeAssignment(task1, id = 10L)
        val a2 = makeAssignment(task2, id = 11L)
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(a1, a2))

        val result = service.findOverdueForNotification(monday, hour = 7)

        assertEquals(2, result.size)
    }

    @Test
    fun `findOverdueForNotification returns empty when no missed candidates`() {
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(emptyList())

        val result = service.findOverdueForNotification(monday, hour = 13)

        assertTrue(result.isEmpty())
    }
}
