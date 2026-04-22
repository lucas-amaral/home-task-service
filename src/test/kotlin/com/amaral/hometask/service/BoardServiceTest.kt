package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.model.dtos.FamilyConfigDto
import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class BoardServiceTest {

    private val taskRepo: TaskRepository = mock()
    private val assignmentService: AssignmentService = mock()
    private val familyConfigService: FamilyConfigService = mock()
    private val pointLedgerService: PointLedgerService = mock()

    private val service = BoardService(taskRepo, assignmentService, familyConfigService, pointLedgerService)

    private val monday = LocalDate.of(2024, 1, 15)
    private val tuesday = monday.plusDays(1)

    private fun makeTask(
        id: Long,
        type: TaskType = TaskType.DAILY,
        frequency: TaskFrequency = TaskFrequency.DAILY
    ) = Task(
        id = id,
        name = "Task $id",
        type = type,
        frequency = frequency,
        defaultAssignee = Assignee.UNASSIGNED,
        points = 1
    )

    private fun makeAssignment(
        task: Task,
        periodDate: LocalDate? = monday,
        periodWeek: LocalDate? = null
    ) = Assignment(
        id = 10L,
        task = task,
        assignedTo = Assignee.CHILD1,
        periodDate = periodDate,
        periodWeek = periodWeek
    )

    @Test
    fun `getBoard creates daily assignment via assignment service`() {
        val dailyTask = makeTask(id = 1L, frequency = TaskFrequency.DAILY)
        val assignment = makeAssignment(task = dailyTask, periodDate = monday)
        whenever(taskRepo.findByActiveTrueOrderBySortOrderAsc()).thenReturn(listOf(dailyTask))
        whenever(familyConfigService.getFamilyConfig()).thenReturn(FamilyConfigDto("TestChild1", "TestChild2"))
        whenever(pointLedgerService.weekPointsMap(monday)).thenReturn(emptyMap())
        whenever(assignmentService.ensureDailyAssignment(dailyTask, monday)).thenReturn(assignment)

        val board = service.getBoard(monday)

        assertEquals(monday, board.date)
        assertEquals(1, board.assignments.size)
        assertEquals(dailyTask.id, board.assignments[0].taskId)
        verify(assignmentService).ensureDailyAssignment(dailyTask, monday)
    }

    @Test
    fun `getBoard creates weekly assignment using weekStart`() {
        val weeklyTask = makeTask(id = 2L, type = TaskType.WEEKLY, frequency = TaskFrequency.WEEKLY)
        val assignment = makeAssignment(task = weeklyTask, periodDate = null, periodWeek = monday)
        whenever(taskRepo.findByActiveTrueOrderBySortOrderAsc()).thenReturn(listOf(weeklyTask))
        whenever(familyConfigService.getFamilyConfig()).thenReturn(FamilyConfigDto("TestChild1", "TestChild2"))
        whenever(pointLedgerService.weekPointsMap(monday)).thenReturn(emptyMap())
        whenever(assignmentService.ensureWeeklyAssignment(weeklyTask, monday)).thenReturn(assignment)

        val board = service.getBoard(tuesday)

        verify(assignmentService).ensureWeeklyAssignment(weeklyTask, monday)
        assertEquals(1, board.assignments.size)
        assertEquals(weeklyTask.id, board.assignments[0].taskId)
    }
}
