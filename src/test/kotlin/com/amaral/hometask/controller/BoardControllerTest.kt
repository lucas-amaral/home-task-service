package com.amaral.hometask.controller

import com.amaral.hometask.model.dtos.BoardDto
import com.amaral.hometask.service.BoardService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDate

@WebMvcTest(BoardController::class)
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockBean lateinit var boardService: BoardService

    private fun boardDto(date: LocalDate) = BoardDto(
        date = date,
        weekStart = date,
        child1Name = "Alice",
        child2Name = "Bob",
        assignments = emptyList(),
        weekPoints = mapOf("CHILD1" to 5, "CHILD2" to 3)
    )

    @Test
    fun `GET health returns ok`() {
        mvc.get("/api/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.today") { exists() }
            jsonPath("$.weekStart") { exists() }
        }
    }

    @Test
    fun `GET board without date param uses today`() {
        val today = LocalDate.now()
        whenever(boardService.getBoard(today)).thenReturn(boardDto(today))

        mvc.get("/api/board").andExpect {
            status { isOk() }
            jsonPath("$.child1Name") { value("Alice") }
            jsonPath("$.weekPoints.CHILD1") { value(5) }
        }

        verify(boardService).getBoard(today)
    }

    @Test
    fun `GET board with date param passes it through`() {
        val today = LocalDate.of(2024, 1, 15)
        whenever(boardService.getBoard(today)).thenReturn(boardDto(today))

        mvc.get("/api/board?date=2024-01-15").andExpect {
            status { isOk() }
        }

        verify(boardService).getBoard(today)
    }
}
