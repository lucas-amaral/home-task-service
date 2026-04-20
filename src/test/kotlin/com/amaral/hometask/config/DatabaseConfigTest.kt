package com.amaral.hometask.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DatabaseConfigTest {

    @Test
    fun `converts postgresql scheme to jdbc url`() {
        val raw = "postgresql://myuser:mypassword@host.railway.app:5432/railway"
        assertEquals(
            "jdbc:postgresql://host.railway.app:5432/railway",
            DatabaseConfig.toJdbcUrl(raw)
        )
    }

    @Test
    fun `converts postgres scheme (Heroku-style) to jdbc url`() {
        val raw = "postgres://myuser:mypassword@host.railway.app:5432/railway"
        assertEquals(
            "jdbc:postgresql://host.railway.app:5432/railway",
            DatabaseConfig.toJdbcUrl(raw)
        )
    }

    @Test
    fun `extracts username and password correctly`() {
        val raw = "postgresql://myuser:mypassword@host.railway.app:5432/railway"
        val (user, pass) = DatabaseConfig.extractCredentials(raw)
        assertEquals("myuser", user)
        assertEquals("mypassword", pass)
    }

    @Test
    fun `extracts credentials when password contains special characters`() {
        val raw = "postgresql://admin:p%40ssw0rd@host:5432/db"
        val (user, pass) = DatabaseConfig.extractCredentials(raw)
        assertEquals("admin", user)
        assertEquals("p%40ssw0rd", pass)
    }

    @Test
    fun `returns empty credentials when no at-sign present`() {
        val raw = "postgresql://host:5432/db"
        val (user, pass) = DatabaseConfig.extractCredentials(raw)
        assertEquals("", user)
        assertEquals("", pass)
    }
}
