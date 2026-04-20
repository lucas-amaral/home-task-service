package com.amaral.hometask.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * Production DataSource configuration for Railway.
 *
 * Railway injects DATABASE_URL in the Postgres URI format:
 *   postgresql://USER:PASSWORD@HOST:PORT/DBNAME
 *
 * Spring/Hibernate needs the JDBC URL format:
 *   jdbc:postgresql://HOST:PORT/DBNAME
 *
 * This config parses the URI and builds the correct HikariCP DataSource.
 */
@Configuration
@Profile("prod")
class DatabaseConfig(
    @Value("\${DATABASE_URL}") private val databaseUrl: String
) {

    @Bean
    fun dataSource(): DataSource {
        val jdbcUrl = toJdbcUrl(databaseUrl)
        val (user, password) = extractCredentials(databaseUrl)

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
            minimumIdle = 1
            connectionTimeout = 30_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
        }
        return HikariDataSource(config)
    }

    companion object {
        /**
         * Converts:
         *   postgresql://user:pass@host:5432/dbname  →  jdbc:postgresql://host:5432/dbname
         *   postgres://user:pass@host:5432/dbname    →  jdbc:postgresql://host:5432/dbname  (Heroku-style)
         */
        fun toJdbcUrl(raw: String): String {
            val cleaned = raw
                .removePrefix("postgresql://")
                .removePrefix("postgres://")
            // cleaned = "user:pass@host:5432/dbname"
            val atIndex = cleaned.indexOf('@')
            val hostAndDb = if (atIndex >= 0) cleaned.substring(atIndex + 1) else cleaned
            return "jdbc:postgresql://$hostAndDb"
        }

        fun extractCredentials(raw: String): Pair<String, String> {
            val withoutScheme = raw
                .removePrefix("postgresql://")
                .removePrefix("postgres://")
            val atIndex = withoutScheme.indexOf('@')
            if (atIndex < 0) return "" to ""
            val userInfo = withoutScheme.substring(0, atIndex)
            val colonIndex = userInfo.indexOf(':')
            return if (colonIndex >= 0) {
                userInfo.substring(0, colonIndex) to userInfo.substring(colonIndex + 1)
            } else {
                userInfo to ""
            }
        }
    }
}
