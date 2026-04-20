package com.amaral.hometask.service

import com.amaral.hometask.model.*
import com.amaral.hometask.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class HomeTaskServiceTest {

    private val taskRepo: TaskRepository = mock()
    private val assignmentRepo: AssignmentRepository = mock()
    private val ledgerRepo: PointLedgerRepository = mock()
    private val rewardRepo: RewardRepository = mock()
    private val familyConfigRepo: FamilyConfigRepository = mock()

    private val service = HomeTaskService(taskRepo, assignmentRepo, ledgerRepo, rewardRepo, familyConfigRepo)

    private val monday = LocalDate.of(2024, 1, 15)    // a Monday
    private val tuesday = monday.plusDays(1)

    private fun makeTask(
        id: Long = 1L,
        name: String = "Test Task",
        type: TaskType = TaskType.DAILY,
        frequency: TaskFrequency = TaskFrequency.DAILY,
        defaultAssignee: Assignee = Assignee.UNASSIGNED,
        points: Int = 1
    ) = Task(id = id, name = name, type = type, frequency = frequency,
             defaultAssignee = defaultAssignee, points = points)

    private fun makeAssignment(
        id: Long = 10L,
        task: Task = makeTask(),
        assignedTo: Assignee = Assignee.CHILD1,
        periodDate: LocalDate? = monday,
        periodWeek: LocalDate? = null,
        completedAt: LocalDateTime? = null,
        bonusEarned: Boolean = false
    ) = Assignment(id = id, task = task, assignedTo = assignedTo,
                   periodDate = periodDate, periodWeek = periodWeek,
                   completedAt = completedAt, bonusEarned = bonusEarned)

    @BeforeEach
    fun setup() {
        whenever(familyConfigRepo.findById(1L)).thenReturn(
            Optional.of(FamilyConfig(child1Name = "TestChild1", child2Name = "TestChild2"))
        )
        whenever(ledgerRepo.findByWeekStart(any())).thenReturn(emptyList())
    }

    // ── weekStart ──────────────────────────────────────────────────────────

    @Test
    fun `weekStart returns Monday for any day of the week`() {
        assertEquals(monday, service.weekStart(monday))
        assertEquals(monday, service.weekStart(tuesday))
        assertEquals(monday, service.weekStart(monday.plusDays(6)))
    }

    // ── getFamilyConfig ────────────────────────────────────────────────────

    @Test
    fun `getFamilyConfig returns names from repository`() {
        val cfg = service.getFamilyConfig()
        assertEquals("TestChild1", cfg.child1Name)
        assertEquals("TestChild2", cfg.child2Name)
    }

    @Test
    fun `getFamilyConfig returns defaults when no row exists`() {
        whenever(familyConfigRepo.findById(1L)).thenReturn(Optional.empty())
        val cfg = service.getFamilyConfig()
        assertEquals("Child 1", cfg.child1Name)
    }

    // ── createTask ─────────────────────────────────────────────────────────

    @Test
    fun `createTask saves and returns task DTO`() {
        val req = CreateTaskRequest(
            name = "New Task", type = TaskType.DAILY,
            frequency = TaskFrequency.DAILY, points = 2
        )
        val saved = makeTask(id = 99L, name = "New Task", points = 2)
        whenever(taskRepo.save(any<Task>())).thenReturn(saved)

        val dto = service.createTask(req)
        assertEquals(99L, dto.id)
        assertEquals("New Task", dto.name)
        verify(taskRepo).save(any())
    }

    // ── getBoard ───────────────────────────────────────────────────────────

    @Test
    fun `getBoard creates daily assignments for today`() {
        val dailyTask = makeTask(id = 1L, frequency = TaskFrequency.DAILY)
        whenever(taskRepo.findByActiveTrueOrderBySortOrderAsc()).thenReturn(listOf(makeTask(id = 1L, frequency = TaskFrequency.DAILY)))
        whenever(assignmentRepo.findByTaskIdAndPeriodDate(1L, monday)).thenReturn(null)
        val created = makeAssignment(task = dailyTask, periodDate = monday)
        whenever(assignmentRepo.save(any<Assignment>())).thenReturn(created)

        val board = service.getBoard(monday)

        assertEquals(monday, board.date)
        assertEquals(1, board.assignments.size)
        assertEquals(monday, board.assignments[0].periodDate)
        assertEquals(dailyTask.id, board.assignments[0].taskId)
        verify(assignmentRepo).save(any())
    }

    @Test
    fun `getBoard reuses existing daily assignment`() {
        val dailyTask = makeTask(id = 1L, frequency = TaskFrequency.DAILY)
        whenever(taskRepo.findByActiveTrueOrderBySortOrderAsc()).thenReturn(listOf(makeTask(id = 1L, frequency = TaskFrequency.DAILY)))
        whenever(assignmentRepo.findByTaskIdAndPeriodDate(1L, monday)).thenReturn(makeAssignment(task = makeTask(id = 1L), periodDate = monday))

        val board = service.getBoard(monday)

        assertEquals(dailyTask.id, board.assignments[0].taskId)
        verify(assignmentRepo, never()).save(any())
    }

    @Test
    fun `getBoard creates weekly assignment using weekStart`() {
        val weeklyTask = makeTask(id = 2L, type = TaskType.WEEKLY, frequency = TaskFrequency.WEEKLY)
        whenever(taskRepo.findByActiveTrueOrderBySortOrderAsc()).thenReturn(listOf(weeklyTask))
        whenever(assignmentRepo.findByTaskIdAndPeriodDate(2L, monday)).thenReturn(null)
        val created = makeAssignment(task = weeklyTask, periodDate = null, periodWeek = monday)
        whenever(assignmentRepo.save(any<Assignment>())).thenReturn(created)

        // Even if board is fetched on Tuesday, weekly task uses Monday
        val board = service.getBoard(tuesday)

        verify(assignmentRepo).findByTaskIdAndPeriodDate(2L, monday)
        assertEquals(1, board.assignments.size)
        assertEquals(weeklyTask.id, board.assignments[0].taskId)
        verify(assignmentRepo).save(any())
    }

    // ── assignTask ─────────────────────────────────────────────────────────

    @Test
    fun `assignTask creates new assignment when none exists`() {
        val task = makeTask(id = 1L)
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD1, date = monday)
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(task))
        whenever(assignmentRepo.findByTaskIdAndPeriodDate(1L, monday)).thenReturn(null)
        val saved = makeAssignment(assignedTo = Assignee.CHILD1)
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
        whenever(taskRepo.findById(1L)).thenReturn(Optional.of(task))
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD1)
        assertThrows<IllegalArgumentException> { service.assignTask(req) }
    }

    // ── completeAssignment ─────────────────────────────────────────────────

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
    fun `completeAssignment awards bonus point when bonusEarned=true`() {
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

    // ── uncompleteAssignment ───────────────────────────────────────────────

    @Test
    fun `uncompleteAssignment reverses points`() {
        val task = makeTask(points = 3)
        val assignment = makeAssignment(task = task, assignedTo = Assignee.CHILD1,
                                        completedAt = LocalDateTime.now(), bonusEarned = true)
        whenever(assignmentRepo.findById(10L)).thenReturn(Optional.of(assignment))
        whenever(assignmentRepo.save(any<Assignment>())).thenAnswer { it.arguments[0] }
        whenever(ledgerRepo.save(any<PointLedger>())).thenAnswer { it.arguments[0] }

        service.uncompleteAssignment(10L)

        // 3 pts + 1 bonus = 4 reversed
        verify(ledgerRepo).save(argThat { delta == -4 && assignee == Assignee.CHILD1 })
    }

    // ── applyPenalty ───────────────────────────────────────────────────────

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
}
