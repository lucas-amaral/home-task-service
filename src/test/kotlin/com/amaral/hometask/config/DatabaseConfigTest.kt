package com.amaral.hometask.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DatabaseConfigTest {

    private fun parse(url: String) = DatabaseConfig.ParsedUrl.from(url)

    // ── URI format (postgresql:// / postgres://) ───────────────────────────

    @Test
    fun `parses standard postgresql URI from Railway`() {
        val r = parse("postgresql://myuser:mypassword@containers-us.railway.app:5432/railway")
        assertEquals("jdbc:postgresql://containers-us.railway.app:5432/railway", r.jdbcUrl)
        assertEquals("myuser", r.username)
        assertEquals("mypassword", r.password)
    }

    @Test
    fun `parses Heroku-style postgres URI`() {
        val r = parse("postgres://admin:secret@ec2-host.compute-1.amazonaws.com:5432/d8r82725r2kd")
        assertEquals("jdbc:postgresql://ec2-host.compute-1.amazonaws.com:5432/d8r82725r2kd", r.jdbcUrl)
        assertEquals("admin", r.username)
        assertEquals("secret", r.password)
    }

    @Test
    fun `parses URI with special characters in password`() {
        val r = parse("postgresql://user:p%40ss!word@host:5432/db")
        assertEquals("user", r.username)
        assertEquals("p%40ss!word", r.password)
    }

    @Test
    fun `parses URI without credentials`() {
        val r = parse("postgresql://host:5432/db")
        assertEquals("jdbc:postgresql://host:5432/db", r.jdbcUrl)
        assertTrue(r.username.isBlank())
        assertTrue(r.password.isBlank())
    }

    // ── JDBC format (already converted by Railway or passed explicitly) ─────

    @Test
    fun `passes through jdbc URL unchanged`() {
        val url = "jdbc:postgresql://host.railway.app:5432/railway"
        val r = parse(url)
        assertEquals(url, r.jdbcUrl)
    }

    @Test
    fun `extracts credentials from JDBC URL query string`() {
        val r = parse("jdbc:postgresql://host:5432/db?user=alice&password=secret123")
        assertEquals("alice", r.username)
        assertEquals("secret123", r.password)
    }

    @Test
    fun `handles JDBC URL without query params`() {
        val r = parse("jdbc:postgresql://host:5432/db")
        assertTrue(r.username.isBlank())
        assertTrue(r.password.isBlank())
    }

    // ── Railway ${{Postgres.DATABASE_URL}} typical value ─────────────────────

    @Test
    fun `handles the exact format Railway injects via Postgres plugin`() {
        // Railway's Postgres plugin typically injects this format:
        val railwayUrl = "postgresql://postgres:AbCdEfGhIjKl@monorail.proxy.rlwy.net:12345/railway"
        val r = parse(railwayUrl)
        assertEquals("jdbc:postgresql://monorail.proxy.rlwy.net:12345/railway", r.jdbcUrl)
        assertEquals("postgres", r.username)
        assertEquals("AbCdEfGhIjKl", r.password)
    }
}
