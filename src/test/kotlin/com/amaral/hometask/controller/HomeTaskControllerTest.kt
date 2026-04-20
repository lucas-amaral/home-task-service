package com.amaral.hometask.controller

import com.amaral.hometask.model.*
import com.amaral.hometask.service.HomeTaskService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(HomeTaskController::class)
@ActiveProfiles("test")
class HomeTaskControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @MockBean  lateinit var service: HomeTaskService

    private val today = LocalDate.of(2024, 1, 15)
    private val week  = LocalDate.of(2024, 1, 15)

    private fun boardDto() = BoardDto(
        date = today, weekStart = week,
        child1Name = "Alice", child2Name = "Bob",
        assignments = emptyList(),
        weekPoints = mapOf("CHILD1" to 5, "CHILD2" to 3)
    )

    // ── /api/health ────────────────────────────────────────────────────────

    @Test
    fun `GET health returns ok`() {
        whenever(service.today()).thenReturn(today)
        whenever(service.weekStart()).thenReturn(week)

        mvc.get("/api/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
        }
    }

    // ── /api/config ────────────────────────────────────────────────────────

    @Test
    fun `GET config returns family names`() {
        whenever(service.getFamilyConfig()).thenReturn(FamilyConfigDto("Alice", "Bob"))

        mvc.get("/api/config").andExpect {
            status { isOk() }
            jsonPath("$.child1Name") { value("Alice") }
            jsonPath("$.child2Name") { value("Bob") }
        }
    }

    @Test
    fun `PUT config updates family names`() {
        val req = UpdateFamilyConfigRequest("Luisa", "Pedro")
        whenever(service.updateFamilyConfig(any())).thenReturn(FamilyConfigDto("Luisa", "Pedro"))

        mvc.put("/api/config") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(req)
        }.andExpect {
            status { isOk() }
            jsonPath("$.child1Name") { value("Luisa") }
        }
    }

    // ── /api/board ─────────────────────────────────────────────────────────

    @Test
    fun `GET board without date param uses today`() {
        whenever(service.today()).thenReturn(today)
        whenever(service.getBoard(today)).thenReturn(boardDto())

        mvc.get("/api/board").andExpect {
            status { isOk() }
            jsonPath("$.child1Name") { value("Alice") }
            jsonPath("$.weekPoints.CHILD1") { value(5) }
        }
    }

    @Test
    fun `GET board with date param passes it through`() {
        whenever(service.getBoard(today)).thenReturn(boardDto())

        mvc.get("/api/board?date=2024-01-15").andExpect {
            status { isOk() }
        }
        verify(service).getBoard(today)
    }

    // ── /api/tasks ─────────────────────────────────────────────────────────

    @Test
    fun `GET tasks returns list`() {
        val task = TaskDto(1L, "Vacuum", "", TaskType.DAILY, TaskFrequency.DAILY,
                           Assignee.UNASSIGNED, 1, "", "any time", true, 1)
        whenever(service.listTasks()).thenReturn(listOf(task))

        mvc.get("/api/tasks").andExpect {
            status { isOk() }
            jsonPath("$[0].name") { value("Vacuum") }
        }
    }

    // ── /api/assignments ───────────────────────────────────────────────────

    @Test
    fun `POST assign returns updated assignment`() {
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD1, date = today)
        val dto = AssignmentDto(
            id = 10L, taskId = 1L, taskName = "Vacuum",
            taskType = TaskType.DAILY, taskFrequency = TaskFrequency.DAILY,
            assignedTo = Assignee.CHILD1, periodDate = today,
            completed = false, completedAt = null,
            bonusEarned = false, penaltyApplied = false, points = 1
        )
        whenever(service.assignTask(any())).thenReturn(dto)

        mvc.post("/api/assignments/assign") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(req)
        }.andExpect {
            status { isOk() }
            jsonPath("$.assignedTo") { value("CHILD1") }
        }
    }

    @Test
    fun `POST complete marks assignment done`() {
        val req = CompleteRequest(bonusEarned = true)
        val dto = AssignmentDto(
            id = 10L, taskId = 1L, taskName = "Vacuum",
            taskType = TaskType.DAILY, taskFrequency = TaskFrequency.DAILY,
            assignedTo = Assignee.CHILD1, periodDate = today,
            completed = true, completedAt = LocalDateTime.now(),
            bonusEarned = true, penaltyApplied = false, points = 1
        )
        whenever(service.completeAssignment(eq(10L), any())).thenReturn(dto)

        mvc.post("/api/assignments/10/complete") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(req)
        }.andExpect {
            status { isOk() }
            jsonPath("$.completed") { value(true) }
            jsonPath("$.bonusEarned") { value(true) }
        }
    }

    // ── /api/rewards ───────────────────────────────────────────────────────

    @Test
    fun `GET rewards returns list`() {
        whenever(service.listRewards()).thenReturn(
            listOf(RewardDto(1L, "Movie night", 5, "🎬"))
        )

        mvc.get("/api/rewards").andExpect {
            status { isOk() }
            jsonPath("$[0].pointsCost") { value(5) }
        }
    }
}
