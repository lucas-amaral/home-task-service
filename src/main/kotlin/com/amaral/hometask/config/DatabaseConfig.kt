package com.amaral.hometask.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * Production DataSource for Railway.
 *
 * Why we disable Spring Boot's DataSource autoconfiguration here:
 * Spring Boot will try to create its own DataSource from spring.datasource.*
 * properties BEFORE our @Bean method runs. When DATABASE_URL is a URI like
 * postgresql://user:pass@host/db, Boot sees it is not a JDBC URL and
 * fails immediately with "Unable to determine Dialect".
 *
 * By excluding DataSourceAutoConfiguration we take full ownership of the
 * DataSource in this profile, and Spring Boot never tries to create one itself.
 *
 * Railway's ${{Postgres.DATABASE_URL}} can arrive in two formats:
 *   (A) URI  – postgresql://user:pass@host:5432/dbname
 *   (B) JDBC – jdbc:postgresql://host:5432/dbname?user=u&password=p
 * Both are handled by ParsedUrl.from().
 */
@Configuration
@Profile("prod")
@EnableAutoConfiguration(exclude = [
    DataSourceAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class
])
class DatabaseConfig(
    @Value("\${DATABASE_URL}") private val databaseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun dataSource(): DataSource {
        val parsed = ParsedUrl.from(databaseUrl)
        log.info("Connecting to database: {}", parsed.jdbcUrl)

        val config = HikariConfig().apply {
            jdbcUrl           = parsed.jdbcUrl
            username          = parsed.username.ifBlank { null }
            password          = parsed.password.ifBlank { null }
            driverClassName   = "org.postgresql.Driver"
            maximumPoolSize   = 5
            minimumIdle       = 1
            connectionTimeout = 30_000
            idleTimeout       = 600_000
            maxLifetime       = 1_800_000
            // Give Railway's internal DNS up to 60 s to resolve on cold start
            initializationFailTimeout = 60_000
        }

        return HikariDataSource(config)
    }

    // ── URL parsing ──────────────────────────────────────────────────────────

    data class ParsedUrl(val jdbcUrl: String, val username: String, val password: String) {
        companion object {
            fun from(raw: String): ParsedUrl = when {
                raw.startsWith("jdbc:")          -> fromJdbc(raw)
                raw.startsWith("postgresql://")  -> fromUri(raw)
                raw.startsWith("postgres://")    -> fromUri(raw)
                else -> ParsedUrl(raw, "", "")   // passthrough, will likely fail loudly
            }

            /** jdbc:postgresql://host/db?user=u&password=p → extract credentials */
            private fun fromJdbc(raw: String): ParsedUrl {
                val qIdx = raw.indexOf('?')
                val params = if (qIdx >= 0) raw.substring(qIdx + 1) else ""
                val map = params.split('&')
                    .mapNotNull { it.split('=').takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }
                    .toMap()
                return ParsedUrl(raw, map["user"] ?: map["username"] ?: "", map["password"] ?: "")
            }

            /** postgresql://user:pass@host:5432/db → jdbc:postgresql://host:5432/db */
            private fun fromUri(raw: String): ParsedUrl {
                val withoutScheme = raw
                    .removePrefix("postgresql://")
                    .removePrefix("postgres://")

                val atIdx = withoutScheme.indexOf('@')
                val hostAndDb = if (atIdx >= 0) withoutScheme.substring(atIdx + 1) else withoutScheme

                val (user, pass) = if (atIdx >= 0) {
                    val userInfo = withoutScheme.substring(0, atIdx)
                    val colonIdx = userInfo.indexOf(':')
                    if (colonIdx >= 0)
                        userInfo.substring(0, colonIdx) to userInfo.substring(colonIdx + 1)
                    else
                        userInfo to ""
                } else "" to ""

                return ParsedUrl("jdbc:postgresql://$hostAndDb", user, pass)
            }
        }
    }
}
