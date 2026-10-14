package com.amaral.hometask.service

import com.amaral.hometask.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** What a run of the daily routine did. */
data class DailyRoutineResult(
    val closedDate: LocalDate,
    val missedMarked: Int,
    val preparedDate: LocalDate,
    val assignmentsReady: Int,
)

/**
 * The turn of the day: the day that ended is closed (every task not done gets marked as missed,
 * with its occurrence) and the day that begins gets its tasks created.
 *
 * Both halves are safe to repeat, so the routine can be run by the internal scheduler and by the
 * external cron, in any order, any number of times.
 */
@Service
class DailyRoutineService(
    private val boardService: BoardService,
    private val assignmentService: AssignmentService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun run(today: LocalDate = DateTimeUtils.today()): DailyRoutineResult {
        val missed = closeDay(today.minusDays(1))
        val ready = prepareDay(today)
        log.info("Daily routine: {} missed on {}, {} assignments ready for {}", missed, today.minusDays(1), ready, today)
        return DailyRoutineResult(today.minusDays(1), missed, today, ready)
    }

    /**
     * Marks as missed what was not done on [date]. The tasks of that day are created first:
     * a day on which nobody opened the board must still count.
     */
    @Transactional
    fun closeDay(date: LocalDate): Int {
        boardService.ensureAssignments(date)
        return assignmentService.applyMissedDeadlinePenalties(date)
    }

    /** Creates the tasks of [date], so they are there before anyone opens the board. */
    @Transactional
    fun prepareDay(date: LocalDate): Int = boardService.ensureAssignments(date).size
}
