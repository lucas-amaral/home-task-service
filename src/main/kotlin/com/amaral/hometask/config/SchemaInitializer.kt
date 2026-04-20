package com.amaral.hometask.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

/**
 * Applies the partial unique indexes defined in schema-postgres.sql on startup.
 *
 * Spring Boot's ddl-auto=update creates columns and tables but does NOT
 * create custom indexes. We run the SQL manually here so the ON CONFLICT
 * clauses in AssignmentRepository always have a valid index to reference.
 *
 * The SQL uses IF NOT EXISTS so it is safe to run on every startup.
 */
@Configuration
@Profile("prod")
class SchemaInitializer(private val dataSource: DataSource) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun applyPartialIndexes() = ApplicationRunner {
        val sql = listOf(
            """CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_daily
               ON assignments (task_id, period_date)
               WHERE period_date IS NOT NULL""",
            """CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_weekly
               ON assignments (task_id, period_week)
               WHERE period_week IS NOT NULL"""
        )

        dataSource.connection.use { conn ->
            sql.forEach { ddl ->
                try {
                    conn.createStatement().use { stmt ->
                        stmt.execute(ddl.trimIndent())
                        log.info("Schema: applied index — ${ddl.lines().first().trim()}")
                    }
                } catch (e: Exception) {
                    // Non-fatal: index may already exist with a different definition
                    log.warn("Schema index skipped (${e.message})")
                }
            }
        }
    }
}
