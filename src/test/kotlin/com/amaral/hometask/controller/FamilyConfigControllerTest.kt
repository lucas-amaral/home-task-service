package com.amaral.hometask.controller

import com.amaral.hometask.model.UpdateFamilyConfigRequest
import com.amaral.hometask.model.dtos.FamilyConfigDto
import com.amaral.hometask.service.FamilyConfigService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@WebMvcTest(FamilyConfigController::class)
@ActiveProfiles("test")
class FamilyConfigControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @MockBean lateinit var service: FamilyConfigService

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
}
