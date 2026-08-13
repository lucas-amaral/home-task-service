package com.amaral.hometask.service

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.PointLedger
import com.amaral.hometask.repository.PointLedgerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class PointLedgerServiceTest {

    private val ledgerRepo: PointLedgerRepository = mock()
    private val service = PointLedgerService(ledgerRepo)

    private val monday = LocalDate.of(2024, 1, 15)

    private fun entry(assignee: Assignee, delta: Int) =
        PointLedger(assignee = assignee, weekStart = monday, delta = delta)

    @Test
    fun `weekPointsMap returns raw negative sum without clamping to zero`() {
        whenever(ledgerRepo.findByWeekStart(monday)).thenReturn(
            listOf(entry(Assignee.CHILD1, -1), entry(Assignee.CHILD1, -1), entry(Assignee.CHILD2, -1))
        )

        val result = service.weekPointsMap(monday)

        assertEquals(-2, result["CHILD1"])
        assertEquals(-1, result["CHILD2"])
    }

    @Test
    fun `weeklyStatus with zero occurrences has no active consequences`() {
        whenever(ledgerRepo.findByWeekStart(monday)).thenReturn(emptyList())

        val status = service.weeklyStatus(monday, "Clara", "Bernardo")

        val clara = status.first { it.assignee == Assignee.CHILD1 }
        assertEquals(0, clara.occurrenceCount)
        assertTrue(clara.consequences.none { it.active })
    }

    @Test
    fun `weeklyStatus activates only the friends and allowance rungs at 3 occurrences`() {
        whenever(ledgerRepo.findByWeekStart(monday)).thenReturn(
            listOf(entry(Assignee.CHILD1, -1), entry(Assignee.CHILD1, -1), entry(Assignee.CHILD1, -1))
        )

        val status = service.weeklyStatus(monday, "Clara", "Bernardo")
        val clara = status.first { it.assignee == Assignee.CHILD1 }

        assertEquals(3, clara.occurrenceCount)
        assertTrue(clara.consequences[0].active) // 1: registered
        assertTrue(clara.consequences[1].active) // 2: no friends
        assertTrue(clara.consequences[2].active) // 3: no allowance
        assertFalse(clara.consequences[3].active) // 4: no phone screen — not yet
    }

    @Test
    fun `weeklyStatus caps occurrence count at zero when reversals exceed penalties`() {
        // e.g. a penalty applied then reversed same week nets to 0, never negative
        whenever(ledgerRepo.findByWeekStart(monday)).thenReturn(
            listOf(entry(Assignee.CHILD2, -1), entry(Assignee.CHILD2, 1))
        )

        val status = service.weeklyStatus(monday, "Clara", "Bernardo")
        val bernardo = status.first { it.assignee == Assignee.CHILD2 }

        assertEquals(0, bernardo.occurrenceCount)
    }

    @Test
    fun `weeklyStatus unlocks all six rungs at 6 or more occurrences`() {
        whenever(ledgerRepo.findByWeekStart(monday)).thenReturn(
            (1..7).map { entry(Assignee.CHILD1, -1) }
        )

        val status = service.weeklyStatus(monday, "Clara", "Bernardo")
        val clara = status.first { it.assignee == Assignee.CHILD1 }

        assertEquals(7, clara.occurrenceCount)
        assertTrue(clara.consequences.all { it.active })
    }
}
