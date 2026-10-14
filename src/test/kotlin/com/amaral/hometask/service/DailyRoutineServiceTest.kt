package com.amaral.hometask.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class DailyRoutineServiceTest {

    private val boardService: BoardService = mock()
    private val assignmentService: AssignmentService = mock()
    private val routine = DailyRoutineService(boardService, assignmentService)

    private val tuesday = LocalDate.of(2024, 1, 16)
    private val monday = tuesday.minusDays(1)

    @Test
    fun `closes yesterday after creating its tasks, then prepares today`() {
        whenever(boardService.ensureAssignments(monday)).thenReturn(emptyList())
        whenever(boardService.ensureAssignments(tuesday)).thenReturn(emptyList())
        whenever(assignmentService.applyMissedDeadlinePenalties(monday)).thenReturn(2)

        val result = routine.run(tuesday)

        assertEquals(DailyRoutineResult(monday, 2, tuesday, 0), result)
        // A day nobody opened must have its tasks before they are judged.
        inOrder(boardService, assignmentService) {
            verify(boardService).ensureAssignments(monday)
            verify(assignmentService).applyMissedDeadlinePenalties(monday)
            verify(boardService).ensureAssignments(tuesday)
        }
    }
}
