package com.amaral.hometask.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

/**
 * Applies schema changes that Hibernate's ddl-auto=update cannot handle:
 *  - Partial unique indexes (ON CONFLICT clauses for upsert operations)
 *  - ADD COLUMN IF NOT EXISTS for new fields introduced after initial deployment
 *
 * All statements are idempotent — safe to run on every startup.
 */
@Configuration
@Profile("prod")
class SchemaInitializer(private val dataSource: DataSource) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun applySchemaChanges() = ApplicationRunner {
        val migrations = listOf(
            // ── Partial unique indexes ────────────────────────────────────────
            """CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_daily
               ON assignments (task_id, period_date)
               WHERE period_date IS NOT NULL""",

            """CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_weekly
               ON assignments (task_id, period_week)
               WHERE period_week IS NOT NULL""",

            // ── Feature: task deadline date ──────────────────────────────────
            // Adds the column only if it doesn't already exist (PostgreSQL 9.6+)
            """ALTER TABLE tasks ADD COLUMN IF NOT EXISTS deadline_date TIMESTAMP""",

            // ── Feature: family WhatsApp phone numbers ───────────────────────
            """ALTER TABLE family_config ADD COLUMN IF NOT EXISTS child1_phone VARCHAR(64) NOT NULL DEFAULT ''""",
            """ALTER TABLE family_config ADD COLUMN IF NOT EXISTS child2_phone VARCHAR(64) NOT NULL DEFAULT ''"""
        )

        dataSource.connection.use { conn ->
            migrations.forEach { ddl ->
                try {
                    conn.createStatement().use { stmt ->
                        stmt.execute(ddl.trimIndent())
                        log.info("Schema migration applied: ${ddl.lines().first().trim()}")
                    }
                } catch (e: Exception) {
                    // Non-fatal: index / column may already exist
                    log.warn("Schema migration skipped (${e.message?.take(120)})")
                }
            }
        }
    }
}
