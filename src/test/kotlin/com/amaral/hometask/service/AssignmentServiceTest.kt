package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.AssignRequest
import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.CompleteRequest
import com.amaral.hometask.model.PointLedger
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.repository.AssignmentRepository
import com.amaral.hometask.repository.PointLedgerRepository
import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class AssignmentServiceTest {

    private val assignmentRepo: AssignmentRepository = mock()
    private val taskRepo: TaskRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val familyConfigService: FamilyConfigService = mock()
    private val whatsAppNotifier: WhatsAppNotifier = mock()

    private val service = AssignmentService(assignmentRepo, taskRepo, ledgerRepo, familyConfigService, whatsAppNotifier)

    private val monday = LocalDate.of(2024, 1, 15)

    private fun makeTask(
        id: Long = 1L,
        name: String = "Test Task",
        type: TaskType = TaskType.DAILY,
        frequency: TaskFrequency = TaskFrequency.DAILY,
        defaultAssignee: Assignee = Assignee.UNASSIGNED,
        points: Int = 1
    ) = Task(
        id = id,
        name = name,
        type = type,
        frequency = frequency,
        defaultAssignee = defaultAssignee,
        points = points
    )

    private fun makeAssignment(
        id: Long = 10L,
        task: Task = makeTask(),
        assignedTo: Assignee = Assignee.CHILD1,
        periodDate: LocalDate? = monday,
        periodWeek: LocalDate? = null,
        completedAt: LocalDateTime? = null,
        bonusEarned: Boolean = false,
        penaltyApplied: Boolean = false,
        missedDeadline: Boolean = false,
        deleted: Boolean? = false
    ) = Assignment(
        id = id,
        task = task,
        assignedTo = assignedTo,
        periodDate = periodDate,
        periodWeek = periodWeek,
        completedAt = completedAt,
        bonusEarned = bonusEarned,
        penaltyApplied = penaltyApplied,
        missedDeadline = missedDeadline,
        deleted = deleted
    )

    @Test
    fun `assignTask creates new assignment when none exists`() {
        val task = makeTask(id = 1L)
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD1, date = monday)
        val saved = makeAssignment(assignedTo = Assignee.CHILD1)
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(task))
        whenever(assignmentRepo.findAllByTaskIdAndPeriodDate(1L, monday)).thenReturn(emptyList())
        whenever(assignmentRepo.save(any<Assignment>())).thenReturn(saved)

        val dto = service.assignTask(req)

        assertEquals(Assignee.CHILD1, dto.assignedTo)
        verify(assignmentRepo).save(any())
    }

    @Test
    fun `assignTask throws when task not found`() {
        whenever(taskRepo.findById(999L)).thenReturn(Optional.empty())
        val req = AssignRequest(taskId = 999L, assignedTo = Assignee.CHILD1, date = monday)

        assertThrows<NoSuchElementException> { service.assignTask(req) }
    }

    @Test
    fun `assignTask requires date or weekStart`() {
        val task = makeTask()
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD1)
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(task))

        assertThrows<IllegalArgumentException> { service.assignTask(req) }
    }

    @Test
    fun `completeAssignment awards points and sets completedAt`() {
        val task = makeTask(points = 2)
        val assignment = makeAssignment(task = task, assignedTo = Assignee.CHILD1)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }

        service.completeAssignment(10L, CompleteRequest(bonusEarned = false))

        verify(ledgerRepo).save(argThat { delta == 2 && assignee == Assignee.CHILD1 })
    }

    @Test
    fun `completeAssignment awards bonus point when bonusEarned is true`() {
        val task = makeTask(points = 1)
        val assignment = makeAssignment(task = task, assignedTo = Assignee.CHILD2)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }

        service.completeAssignment(10L, CompleteRequest(bonusEarned = true))

        verify(ledgerRepo).save(argThat { delta == 2 && assignee == Assignee.CHILD2 })
    }

    @Test
    fun `completeAssignment awards points to both children for BOTH assignee`() {
        val task = makeTask(points = 1)
        val assignment = makeAssignment(task = task, assignedTo = Assignee.BOTH)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }

        service.completeAssignment(10L, CompleteRequest())

        verify(ledgerRepo).save(argThat { assignee == Assignee.CHILD1 })
        verify(ledgerRepo).save(argThat { assignee == Assignee.CHILD2 })
    }

    @Test
    fun `completeAssignment throws when already completed`() {
        val assignment = makeAssignment(completedAt = LocalDateTime.now())
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))

        assertThrows<IllegalStateException> { service.completeAssignment(10L, CompleteRequest()) }
    }

    @Test
    fun `uncompleteAssignment reverses points`() {
        val task = makeTask(points = 3)
        val assignment = makeAssignment(
            task = task,
            assignedTo = Assignee.CHILD1,
            completedAt = LocalDateTime.now(),
            bonusEarned = true
        )
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }

        service.uncompleteAssignment(10L)

        verify(ledgerRepo).save(argThat { delta == -4 && assignee == Assignee.CHILD1 })
    }

    @Test
    fun `applyPenalty deducts 1 point from correct child`() {
        val task = makeTask()
        val assignment = makeAssignment(task = task, assignedTo = Assignee.CHILD2)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }

        service.applyPenalty(10L)

        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD2 })
    }

    @Test
    fun `deleteAssignment creates tombstone instead of hard deleting`() {
        val assignment = makeAssignment()
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }

        service.deleteAssignment(10L)

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

    @Test
    fun `assignTask revives deleted assignment for same period`() {
        val task = makeTask(id = 1L)
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD2, date = monday)
        val deletedAssignment = makeAssignment(assignedTo = Assignee.CHILD1, deleted = true)
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(task))
        whenever(assignmentRepo.findAllByTaskIdAndPeriodDate(1L, monday)).thenReturn(listOf(deletedAssignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }

        val dto = service.assignTask(req)

        assertEquals(Assignee.CHILD2, dto.assignedTo)
        verify(assignmentRepo).save(argThat {
            id == deletedAssignment.id &&
            deleted == false &&
            assignedTo == Assignee.CHILD2
        })
    }
}
