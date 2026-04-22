package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.dtos.PointLedgerDto
import com.amaral.hometask.repository.PointLedgerRepository
import org.springframework.stereotype.Service

@Service
class PointLedgerService(
    private val ledgerRepo: PointLedgerRepository
) {

    fun getPointsHistory(): List<PointLedgerDto> =
        ledgerRepo.findAllOrderByWeekDesc()
            .groupBy { it.weekStart to it.assignee }
            .map { (key, entries) -> PointLedgerDto(key.second, key.first, entries.sumOf { it.delta }) }
            .sortedByDescending { it.weekStart }

    fun weekPointsMap(weekStart: java.time.LocalDate): Map<String, Int> {
        val entries = ledgerRepo.findByWeekStart(weekStart)
        return listOf(Assignee.CHILD1, Assignee.CHILD2).associate { person ->
            person.name to maxOf(0, entries.filter { it.assignee == person }.sumOf { it.delta })
        }
    }
}

