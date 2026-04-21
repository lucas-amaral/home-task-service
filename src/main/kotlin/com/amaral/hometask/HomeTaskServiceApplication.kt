package com.amaral.hometask

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HomeTaskServiceApplication

fun main(args: Array<String>) {
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
