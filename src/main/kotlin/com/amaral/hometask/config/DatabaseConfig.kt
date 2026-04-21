package com.amaral.hometask.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import jakarta.annotation.PostConstruct

/**
 * Normaliza a DATABASE_URL do Railway para o formato JDBC antes do Spring Boot
 * inicializar o DataSource.
 *
 * O Railway injeta DATABASE_URL em dois formatos possíveis:
 *   (A) URI  – postgresql://user:pass@host:5432/dbname
 *   (B) JDBC – jdbc:postgresql://host:5432/dbname
 *
 * O Spring Boot / HikariCP exige o formato (B) em spring.datasource.url.
 * Este bean converte (A) → (B) quando necessário, sem desabilitar o autoconfig.
 */
@Configuration
@Profile("prod")
class DatabaseConfig(private val env: ConfigurableEnvironment) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun normalizeDataSourceUrl() {
        val raw = env.getProperty("spring.datasource.url") ?: return

        val jdbcUrl = when {
            raw.startsWith("jdbc:")           -> raw  // já está no formato correto
            raw.startsWith("postgresql://")   -> "jdbc:$raw"
            raw.startsWith("postgres://")     -> raw.replaceFirst("postgres://", "jdbc:postgresql://")
            else                              -> raw
        }

        if (jdbcUrl != raw) {
            log.info("DatabaseConfig: converteu URL para formato JDBC")
            val overrides = mapOf("spring.datasource.url" to jdbcUrl)
            env.propertySources.addFirst(MapPropertySource("railwayUrlNormalized", overrides))
        }
    }
}
