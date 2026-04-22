package com.amaral.hometask.controller

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.model.dtos.TaskDto
import com.amaral.hometask.service.TaskService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(TaskController::class)
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockBean lateinit var service: TaskService

    @Test
    fun `GET tasks returns list`() {
        val task = TaskDto(
            id = 1L,
            name = "Vacuum",
            description = "",
            type = TaskType.DAILY,
            frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.UNASSIGNED,
            points = 1,
            timeWindow = "",
            deadline = "any time",
            active = true,
            sortOrder = 1
        )
        whenever(service.listTasks()).thenReturn(listOf(task))

        mvc.get("/api/tasks").andExpect {
            status { isOk() }
            jsonPath("$[0].name") { value("Vacuum") }
        }
    }
}
