package com.amaral.hometask.config

import com.amaral.hometask.repository.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.test.context.ActiveProfiles

/**
 * The app boots with spring.main.lazy-initialization=true, where a bean nobody injects is never
 * created. Spring Boot already keeps beans with @Scheduled methods eager (and the schedulers also
 * carry @Lazy(false)); DataSeeder only runs at boot because of its @Lazy(false). This test fails if
 * a job stops being registered or the seed stops running.
 */
@SpringBootTest(properties = ["spring.main.lazy-initialization=true"])
@ActiveProfiles("test")
class SchedulingStartupTest {

    @Autowired lateinit var holders: List<ScheduledTaskHolder>
    @Autowired lateinit var taskRepo: TaskRepository

    @Test
    fun `scheduled jobs are registered with lazy initialization on`() {
        val jobs = holders.flatMap { it.scheduledTasks }
            // toString() is "package.Class.method" (the runnable itself is wrapped since Spring 6.2)
            .map { it.task.runnable.toString().split('.').takeLast(2).joinToString(".") }
            .toSet()

        assertEquals(
            setOf(
                "DeadlinePenaltyScheduler.applyDailyPenalties",
                "DeadlineNotificationScheduler.checkHourlyDeadlines",
                "ReminderScheduler.noon",
                "ReminderScheduler.evening",
            ),
            jobs,
        )
    }

    @Test
    fun `data seeder runs at boot with lazy initialization on`() {
        assertTrue(taskRepo.count() > 0)
    }
}
