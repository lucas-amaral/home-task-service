package com.amaral.hometask.controller

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.AssignRequest
import com.amaral.hometask.model.CompleteRequest
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.model.dtos.AssignmentDto
import com.amaral.hometask.service.AssignmentService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AssignmentController::class)
@ActiveProfiles("test")
class AssignmentControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @MockBean lateinit var service: AssignmentService

    private val today = LocalDate.of(2024, 1, 15)

    @Test
    fun `POST assign returns updated assignment`() {
        val req = AssignRequest(taskId = 1L, assignedTo = Assignee.CHILD1, date = today)
        val dto = AssignmentDto(
            id = 10L,
            taskId = 1L,
            taskName = "Vacuum",
            taskDescription = "Use the vacuum",
            taskType = TaskType.DAILY,
            taskFrequency = TaskFrequency.DAILY,
            assignedTo = Assignee.CHILD1,
            periodDate = today,
            completed = false,
            completedAt = null,
            bonusEarned = false,
            penaltyApplied = false,
            points = 1
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
            id = 10L,
            taskId = 1L,
            taskName = "Vacuum",
            taskDescription = "Use the vacuum",
            taskType = TaskType.DAILY,
            taskFrequency = TaskFrequency.DAILY,
            assignedTo = Assignee.CHILD1,
            periodDate = today,
            completed = true,
            completedAt = LocalDateTime.now(),
            bonusEarned = true,
            penaltyApplied = false,
            points = 1
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
}
