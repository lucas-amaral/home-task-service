package com.amaral.hometask.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.net.URI
import javax.sql.DataSource

/**
 * Most Postgres hosts (Railway, Render, Neon, Supabase, ...) expose a
 * connection string in the standard `postgres://user:pass@host:port/db`
 * form — NOT a JDBC url. Spring's `spring.datasource.url` needs
 * `jdbc:postgresql://host:port/db` plus separate username/password, so we
 * parse whatever `DATABASE_URL` we're given here rather than assuming one
 * specific host's quirks (or requiring it to be hand-edited per provider).
 *
 * Neon/Supabase also require `sslmode=require`; we add it automatically if
 * the URL doesn't already specify a sslmode.
 */
@Configuration
@Profile("prod")
class DatabaseConfig(
    @Value("\${DATABASE_URL:}") private val rawDatabaseUrl: String
) {

    @Bean
    fun dataSource(): DataSource {
        require(rawDatabaseUrl.isNotBlank()) {
            "DATABASE_URL is not set. Point it at your Postgres instance, " +
                "e.g. postgres://user:pass@host:5432/dbname"
        }

        val uri = URI(rawDatabaseUrl)
        val (username, password) = (uri.userInfo ?: "").split(":", limit = 2)
            .let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }

        val query = uri.query
        val hasSslMode = query?.contains("sslmode") == true
        val fullQuery = when {
            query.isNullOrBlank() -> "sslmode=require"
            hasSslMode -> query
            else -> "$query&sslmode=require"
        }

        val jdbcUrl = "jdbc:postgresql://${uri.host}:${if (uri.port == -1) 5432 else uri.port}${uri.path}?$fullQuery"

        return DataSourceBuilder.create()
            .type(HikariDataSource::class.java)
            .url(jdbcUrl)
            .username(username)
            .password(password)
            .driverClassName("org.postgresql.Driver")
            .build()
    }
}
