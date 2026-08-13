package com.amaral.hometask.service

/**
 * The fixed, automatic escalation of consequences per occurrence count
 * within a single week. Occurrence = a task not done, done late, or done
 * incompletely (each worth −1 point). No negotiation — the ladder applies
 * purely based on the accumulated count for the week.
 */
object ConsequenceLadder {
    data class Rung(val level: Int, val description: String)

    val RUNGS: List<Rung> = listOf(
        Rung(1, "Ocorrência registrada"),
        Rung(2, "Perde o direito de fazer atividades com amigos nesta semana"),
        Rung(3, "Perde a mesada desta semana"),
        Rung(4, "Perde o tempo de tela do celular"),
        Rung(5, "Perde o direito de usar fones de ouvido"),
        Rung(6, "Perde todo o tempo de tela"),
    )

    const val MAX_LEVEL = 6
}
