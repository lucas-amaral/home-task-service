package com.amaral.hometask.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class DatabaseConfigTest {

    private fun normalize(url: String): String {
        val mockEnv = MockEnvironment().apply {
            setProperty("spring.datasource.url", url)
        }
        DatabaseConfig(mockEnv).normalizeDataSourceUrl()
        return mockEnv.getRequiredProperty("spring.datasource.url")
    }

    @Test
    fun `mantém URL JDBC inalterada`() {
        val url = "jdbc:postgresql://host.railway.app:5432/railway"
        assertEquals(url, normalize(url))
    }

    @Test
    fun `converte postgresql URI para JDBC`() {
        assertEquals(
            "jdbc:postgresql://user:pass@host:5432/railway",
            normalize("postgresql://user:pass@host:5432/railway")
        )
    }

    @Test
    fun `converte postgres URI curto para JDBC`() {
        assertEquals(
            "jdbc:postgresql://user:pass@host:5432/db",
            normalize("postgres://user:pass@host:5432/db")
        )
    }

    @Test
    fun `converte formato típico injetado pelo Railway`() {
        assertEquals(
            "jdbc:postgresql://postgres:AbCdEf@monorail.proxy.rlwy.net:12345/railway",
            normalize("postgresql://postgres:AbCdEf@monorail.proxy.rlwy.net:12345/railway")
        )
    }
}
