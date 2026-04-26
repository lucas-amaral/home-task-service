package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.repository.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate

class DeadlinePenaltySchedulerTest {

    private val assignmentRepo: AssignmentRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val taskRepo: TaskRepository = mock()
    private val familyConfigService: FamilyConfigService = mock()

    private val service = AssignmentService(assignmentRepo, taskRepo, ledgerRepo, familyConfigService)

    private val monday  = LocalDate.of(2024, 1, 15)
    private val tuesday = monday.plusDays(1)

    private fun makeTask(id: Long = 1L) = Task(
        id = id, name = "Test", type = TaskType.DAILY,
        frequency = TaskFrequency.DAILY, points = 1
    )

    @BeforeEach
    fun setup() {
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
    }

    // O service chama findMissedCandidates — não findAllForWeek

    @Test
    fun `applies penalty to incomplete CHILD1 assignment`() {
        val task = makeTask()
        val assignment = Assignment(
            id = 1L, task = task, assignedTo = Assignee.CHILD1,
            periodDate = monday, completedAt = null,
            penaltyApplied = false, missedDeadline = false
        )
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(assignment))

        val count = service.applyMissedDeadlinePenalties(monday)

        assertEquals(1, count)
        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD1 })
        verify(assignmentRepo).save(argThat { missedDeadline && penaltyApplied })
    }

    @Test
    fun `skips already completed assignments`() {
        // findMissedCandidates já filtra completedAt IS NULL — retorna vazio
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(emptyList())

        val count = service.applyMissedDeadlinePenalties(monday)

        assertEquals(0, count)
        verify(ledgerRepo, never()).save(any())
    }

    @Test
    fun `skips UNASSIGNED assignments`() {
        // findMissedCandidates já filtra assignedTo IN ('CHILD1','CHILD2') — retorna vazio
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(emptyList())

        val count = service.applyMissedDeadlinePenalties(monday)

        assertEquals(0, count)
        verify(ledgerRepo, never()).save(any())
    }

    @Test
    fun `skips already penalised assignments`() {
        // findMissedCandidates já filtra penaltyApplied = false — retorna vazio
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(emptyList())

        val count = service.applyMissedDeadlinePenalties(monday)

        assertEquals(0, count)
    }

    @Test
    fun `penalises both CHILD1 and CHILD2 independently`() {
        val task1 = makeTask(id = 1L)
        val task2 = makeTask(id = 2L)
        val a1 = Assignment(id = 1L, task = task1, assignedTo = Assignee.CHILD1,
                            periodDate = monday, penaltyApplied = false, missedDeadline = false)
        val a2 = Assignment(id = 2L, task = task2, assignedTo = Assignee.CHILD2,
                            periodDate = monday, penaltyApplied = false, missedDeadline = false)
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(listOf(a1, a2))

        val count = service.applyMissedDeadlinePenalties(monday)

        assertEquals(2, count)
        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD1 })
        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD2 })
    }

    @Test
    fun `returns zero when no assignments exist`() {
        whenever(assignmentRepo.findMissedCandidates(monday, monday)).thenReturn(emptyList())
        assertEquals(0, service.applyMissedDeadlinePenalties(monday))
        verify(ledgerRepo, never()).save(any())
    }
}
