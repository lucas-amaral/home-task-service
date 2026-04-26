package com.amaral.hometask.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import javax.sql.DataSource

/**
 * Applies schema changes before JPA starts.
 *
 * This covers changes that Hibernate's ddl-auto=update cannot handle safely on
 * existing PostgreSQL tables, especially when a new NOT NULL column must be
 * backfilled before the constraint is applied.
 *
 * It also creates partial unique indexes used by ON CONFLICT upserts.
 *
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
    fun schemaMigrationExecutor() = SchemaMigrationExecutor(dataSource, log)

    companion object {
        @Bean
        @JvmStatic
        fun entityManagerFactoryDependsOnSchemaMigration(): BeanFactoryPostProcessor {
            return BeanFactoryPostProcessor { beanFactory ->
                val registry = beanFactory as? BeanDefinitionRegistry ?: return@BeanFactoryPostProcessor
                if (registry.containsBeanDefinition("entityManagerFactory")) {
                    val beanDefinition = registry.getBeanDefinition("entityManagerFactory")
                    val currentDependsOn = beanDefinition.dependsOn?.toList().orEmpty()
                    val updatedDependsOn = (currentDependsOn + "schemaMigrationExecutor").distinct().toTypedArray()
                    beanDefinition.setDependsOn(*updatedDependsOn)
                }
            }
        }
    }
}

class SchemaMigrationExecutor(
    private val dataSource: DataSource,
    private val log: org.slf4j.Logger
) {
    init {
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

            // ── Feature: one-off tasks ───────────────────────────────────────
            """ALTER TABLE tasks ADD COLUMN IF NOT EXISTS one_off BOOLEAN""",
            """UPDATE tasks SET one_off = false WHERE one_off IS NULL""",
            """ALTER TABLE tasks ALTER COLUMN one_off SET DEFAULT false""",
            """ALTER TABLE tasks ALTER COLUMN one_off SET NOT NULL""",

            // ── Feature: family WhatsApp phone numbers ───────────────────────
            """ALTER TABLE family_config ADD COLUMN IF NOT EXISTS child1_phone VARCHAR(64) NOT NULL DEFAULT ''""",
            """ALTER TABLE family_config ADD COLUMN IF NOT EXISTS child2_phone VARCHAR(64) NOT NULL DEFAULT ''""",

            // ── Feature: assignment tombstones for period delete ─────────────
            """ALTER TABLE assignments ADD COLUMN IF NOT EXISTS deleted BOOLEAN""",
            """UPDATE assignments SET deleted = false WHERE deleted IS NULL""",
            """ALTER TABLE assignments ALTER COLUMN deleted SET DEFAULT false""",
            """ALTER TABLE assignments ALTER COLUMN deleted SET NOT NULL"""
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
