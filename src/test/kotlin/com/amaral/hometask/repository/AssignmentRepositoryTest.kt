package com.amaral.hometask.repository

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.PointLedger
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class AssignmentRepositoryTest {

    @Autowired lateinit var taskRepo: TaskRepository
    @Autowired lateinit var assignmentRepo: AssignmentRepository
    @Autowired lateinit var ledgerRepo: PointLedgerRepository

    private val monday = LocalDate.of(2024, 1, 15)
    private val tuesday = monday.plusDays(1)

    private fun saveTask(name: String = "Task", frequency: TaskFrequency = TaskFrequency.DAILY) =
        taskRepo.save(Task(name = name, type = TaskType.DAILY, frequency = frequency, points = 1))

    @Test
    fun `findByTaskIdAndPeriodDate returns correct daily assignment`() {
        val task = saveTask()
        val a = assignmentRepo.save(Assignment(task = task, assignedTo = Assignee.CHILD1, periodDate = monday))

        val found = assignmentRepo.findByTaskIdAndPeriodDate(task.id, monday)
        assertNotNull(found)
        assertEquals(a.id, found!!.id)
    }

    @Test
    fun `findByTaskIdAndPeriodDate returns null for different date`() {
        val task = saveTask()
        assignmentRepo.save(Assignment(task = task, assignedTo = Assignee.CHILD1, periodDate = monday))

        assertNull(assignmentRepo.findByTaskIdAndPeriodDate(task.id, tuesday))
    }

    @Test
    fun `findByTaskIdAndPeriodWeek returns correct weekly assignment`() {
        val task = saveTask(frequency = TaskFrequency.WEEKLY)
        assignmentRepo.save(Assignment(task = task, assignedTo = Assignee.CHILD2, periodWeek = monday))

        val found = assignmentRepo.findByTaskIdAndPeriodWeek(task.id, monday)
        assertNotNull(found)
        assertEquals(Assignee.CHILD2, found!!.assignedTo)
    }

    @Test
    fun `findAllForWeek returns both daily and weekly assignments`() {
        val daily  = saveTask("Daily",  TaskFrequency.DAILY)
        val weekly = saveTask("Weekly", TaskFrequency.WEEKLY)

        assignmentRepo.save(Assignment(task = daily,  assignedTo = Assignee.CHILD1, periodDate = tuesday))
        assignmentRepo.save(Assignment(task = weekly, assignedTo = Assignee.CHILD2, periodWeek = monday))

        val results = assignmentRepo.findAllForWeek(monday, monday.plusDays(7))
        assertEquals(2, results.size)
    }

    @Test
    fun `findCompletedBetween only returns completed assignments`() {
        val task = saveTask()
        assignmentRepo.save(Assignment(task = task, assignedTo = Assignee.CHILD1,
                                       periodDate = monday, completedAt = LocalDateTime.now()))
        assignmentRepo.save(Assignment(task = task, assignedTo = Assignee.CHILD2,
                                       periodDate = tuesday))  // not completed

        val results = assignmentRepo.findCompletedBetween(monday, tuesday.plusDays(1))
        assertEquals(1, results.size)
        assertNotNull(results[0].completedAt)
    }

    @Test
    fun `point ledger aggregates correctly`() {
        ledgerRepo.save(PointLedger(assignee = Assignee.CHILD1, weekStart = monday, delta = 3))
        ledgerRepo.save(PointLedger(assignee = Assignee.CHILD1, weekStart = monday, delta = 1))
        ledgerRepo.save(PointLedger(assignee = Assignee.CHILD2, weekStart = monday, delta = 2))

        val entries = ledgerRepo.findByWeekStart(monday)
        val child1Total = entries.filter { it.assignee == Assignee.CHILD1 }.sumOf { it.delta }
        val child2Total = entries.filter { it.assignee == Assignee.CHILD2 }.sumOf { it.delta }

        assertEquals(4, child1Total)
        assertEquals(2, child2Total)
    }
}
