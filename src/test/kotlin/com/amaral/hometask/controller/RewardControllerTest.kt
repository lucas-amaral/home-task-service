package com.amaral.hometask.controller

import com.amaral.hometask.model.dtos.RewardDto
import com.amaral.hometask.service.RewardService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(RewardController::class)
@ActiveProfiles("test")
class RewardControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockBean lateinit var service: RewardService

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
