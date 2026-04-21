package com.amaral.hometask

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HomeTaskServiceApplication

fun main(args: Array<String>) {
    // O Railway injeta DATABASE_URL como "postgresql://..." (sem prefixo jdbc:).
    // O HikariCP exige "jdbc:postgresql://...". Corrigimos aqui, antes de qualquer
    // inicialização do Spring, sobrescrevendo a variável de ambiente via propriedade
    // de sistema — que tem prioridade máxima na hierarquia do Spring Boot.
    val rawUrl = System.getenv("DATABASE_URL")
    if (rawUrl != null && !rawUrl.startsWith("jdbc:")) {
        val jdbcUrl = when {
            rawUrl.startsWith("postgres://")   -> rawUrl.replaceFirst("postgres://", "jdbc:postgresql://")
            rawUrl.startsWith("postgresql://") -> "jdbc:$rawUrl"
            else                               -> rawUrl
        }
        System.setProperty("spring.datasource.url", jdbcUrl)
    }

    runApplication<HomeTaskServiceApplication>(*args)
}
