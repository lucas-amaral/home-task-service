package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate

class DeadlinePenaltyTest {

    private val assignmentRepo: AssignmentRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val taskRepo: TaskRepository = mock()
    private val familyConfigService: FamilyConfigService = mock()
    private val whatsAppNotifier: WhatsAppNotifier = mock()

    private val service = AssignmentService(assignmentRepo, taskRepo, ledgerRepo, familyConfigService, whatsAppNotifier)

    // A Monday
    private val monday = LocalDate.of(2024, 1, 15)
    private val tuesday = monday.plusDays(1)
    private val sunday = monday.plusDays(6)

    private fun makeTask(id: Long = 1L, frequency: TaskFrequency = TaskFrequency.DAILY) =
        Task(id = id, name = "Task $id", type = TaskType.DAILY, frequency = frequency, points = 1)

    private fun makeAssignment(
        id: Long = 10L,
        task: Task = makeTask(),
        assignedTo: Assignee = Assignee.CHILD1,
        periodDate: LocalDate? = monday,
        periodWeek: LocalDate? = null
    ) = Assignment(id = id, task = task, assignedTo = assignedTo,
                   periodDate = periodDate, periodWeek = periodWeek)

    @BeforeEach
    fun setup() {
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `daily missed assignment gets penalty on any week day`() {
        val a = makeAssignment(periodDate = tuesday)
        whenever(assignmentRepo.findMissedCandidates(tuesday, monday)).thenReturn(listOf(a))

        val count = service.applyMissedDeadlinePenalties(tuesday)

        assertEquals(1, count)
        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD1 })
    }

    @Test
    fun `weekly missed assignment is NOT penalised on a weekday`() {
        val weeklyTask = makeTask(frequency = TaskFrequency.WEEKLY)
        val a = makeAssignment(task = weeklyTask, periodDate = null, periodWeek = monday)
        whenever(assignmentRepo.findMissedCandidates(tuesday, monday)).thenReturn(listOf(a))

        val count = service.applyMissedDeadlinePenalties(tuesday)

        assertEquals(0, count)
        verify(ledgerRepo, never()).save(any())
    }

    @Test
    fun `weekly missed assignment IS penalised on Sunday`() {
        val weeklyTask = makeTask(frequency = TaskFrequency.WEEKLY)
        val a = makeAssignment(task = weeklyTask, periodDate = null, periodWeek = monday)
        whenever(assignmentRepo.findMissedCandidates(sunday, monday)).thenReturn(listOf(a))

        val count = service.applyMissedDeadlinePenalties(sunday)

        assertEquals(1, count)
        verify(ledgerRepo).save(argThat { delta == -1 && assignee == Assignee.CHILD1 })
    }

    @Test
    fun `UNASSIGNED assignment is never penalised`() {
        // findMissedCandidates already filters out UNASSIGNED at the DB level,
        // but even if one slips through the service resolvePersons returns empty
        whenever(assignmentRepo.findMissedCandidates(tuesday, monday)).thenReturn(emptyList())

        val count = service.applyMissedDeadlinePenalties(tuesday)

        assertEquals(0, count)
        verify(ledgerRepo, never()).save(any())
    }

    @Test
    fun `already penalised assignment is not penalised again`() {
        // Already penalised row is excluded by findMissedCandidates (penaltyApplied = false filter)
        whenever(assignmentRepo.findMissedCandidates(tuesday, monday)).thenReturn(emptyList())

        val count = service.applyMissedDeadlinePenalties(tuesday)

        assertEquals(0, count)
    }

    @Test
    fun `returns zero when no candidates found`() {
        whenever(assignmentRepo.findMissedCandidates(tuesday, monday)).thenReturn(emptyList())
        assertEquals(0, service.applyMissedDeadlinePenalties(tuesday))
    }
}
