package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.dtos.ConsequenceDto
import com.amaral.hometask.model.dtos.PointLedgerDto
import com.amaral.hometask.model.dtos.WeeklyStatusDto
import com.amaral.hometask.repository.PointLedgerRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PointLedgerService(
    private val ledgerRepo: PointLedgerRepository
) {

    fun getPointsHistory(): List<PointLedgerDto> =
        ledgerRepo.findAllOrderByWeekDesc()
            .groupBy { it.weekStart to it.assignee }
            .map { (key, entries) -> PointLedgerDto(key.second, key.first, entries.sumOf { it.delta }) }
            .sortedByDescending { it.weekStart }

    /**
     * Points for the week, one entry per child. In the punitive model this is
     * always ≤ 0: it only moves down (−1 per occurrence) and only moves back
     * up when a penalty is manually reversed. We intentionally do NOT clamp
     * at 0 anymore — the raw number is what's shown ("-3 esta semana").
     */
    fun weekPointsMap(weekStart: LocalDate): Map<String, Int> {
        val entries = ledgerRepo.findByWeekStart(weekStart)
        return listOf(Assignee.CHILD1, Assignee.CHILD2).associate { person ->
            person.name to entries.filter { it.assignee == person }.sumOf { it.delta }
        }
    }

    /**
     * Net occurrence count for the week per child (−sum of deltas, floored
     * at 0 so a manually-reversed penalty can't go negative), plus which
     * rungs of the consequence ladder are currently active as a result.
     */
    fun weeklyStatus(weekStart: LocalDate, child1Name: String, child2Name: String): List<WeeklyStatusDto> {
        val entries = ledgerRepo.findByWeekStart(weekStart)
        return listOf(
            Assignee.CHILD1 to child1Name,
            Assignee.CHILD2 to child2Name
        ).map { (person, name) ->
            val net = entries.filter { it.assignee == person }.sumOf { it.delta }
            val occurrences = maxOf(0, -net)
            WeeklyStatusDto(
                assignee = person,
                name = name,
                weekStart = weekStart,
                occurrenceCount = occurrences,
                consequences = ConsequenceLadder.RUNGS.map {
                    ConsequenceDto(it.level, it.description, active = occurrences >= it.level)
                }
            )
        }
    }
}

