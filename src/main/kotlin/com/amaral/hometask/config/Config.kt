package com.amaral.hometask.config

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.FamilyConfig
import com.amaral.hometask.model.Reward
import com.amaral.hometask.model.Task
import com.amaral.hometask.model.TaskFrequency
import com.amaral.hometask.model.TaskType
import com.amaral.hometask.repository.FamilyConfigRepository
import com.amaral.hometask.repository.RewardRepository
import com.amaral.hometask.repository.TaskRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(@Value("\${app.frontend-url}") private val frontendUrl: String) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(frontendUrl, "http://localhost:5173", "http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
    }
}

@Configuration
class DataSeeder(
    private val taskRepo: TaskRepository,
    private val rewardRepo: RewardRepository,
    private val familyConfigRepo: FamilyConfigRepository,
    @Value("\${app.family.child1-name:Clara}") private val child1Name: String,
    @Value("\${app.family.child2-name:Bernardo}") private val child2Name: String
) {
    @Bean
    fun seedData() = ApplicationRunner {
        // Seed family config (only if not already set)
        if (!familyConfigRepo.existsById(1L)) {
            familyConfigRepo.save(FamilyConfig(id = 1L, child1Name = child1Name, child2Name = child2Name))
        }

        if (taskRepo.count() == 0L) {
            taskRepo.saveAll(buildTasks())
        }

        if (rewardRepo.count() == 0L) {
            rewardRepo.saveAll(buildRewards())
        }
    }

    private fun buildTasks() = listOf(
        // ── Daily / Rule ────────────────────────────────────────────────────
        Task(
            name = "Verificar banheiro — $child1Name",
            description = "Tirar roupas íntimas molhadas, toalhas e roupas do banheiro",
            type = TaskType.RULE, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.CHILD1,
            points = 1, timeWindow = "06:30 – 07:30", deadline = "até 07:30", sortOrder = 1
        ),
        Task(
            name = "Verificar banheiro — $child2Name",
            description = "Tirar roupas íntimas molhadas, toalhas e roupas do banheiro",
            type = TaskType.RULE, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.CHILD2,
            points = 1, timeWindow = "06:30 – 07:30", deadline = "até 07:30", sortOrder = 1
        ),
        Task(
            name = "Tirar mesa do café da manhã",
            description = "Juntos: Tirar a mesa e colocar a louça na máquina",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.BOTH,
            points = 1, deadline = "manhã", sortOrder = 2
        ),
        Task(
            name = "Tirar mesa do almoço",
            description = "Juntos: tirar a mesa e ligar/programar a máquina de lavar louça",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.BOTH,
            points = 1, deadline = "até 13:05", sortOrder = 3
        ),
        Task(
            name = "Passar aspirador nas áreas comuns",
            description = "Cozinha, sala, corredor e banheiro",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.UNASSIGNED,
            points = 1, deadline = "qualquer horário", sortOrder = 4
        ),
        Task(
            name = "Itens pessoais fora da sala  — $child1Name",
            description = "Tênis, meias, pijama e mochila não podem ficar na sala",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.CHILD1,
            points = 1, deadline = "até 19:30", sortOrder = 5
        ),
        Task(
            name = "Itens pessoais fora da sala  — $child2Name",
            description = "Tênis, meias, pijama e mochila não podem ficar na sala",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.CHILD2,
            points = 1, deadline = "até 19:30", sortOrder = 5
        ),
        Task(
            name = "Colocar/Tirar mesa do jantar e programar máquina de louça",
            description = "Tirar a mesa e ligar/programar a máquina de lavar louça",
            type = TaskType.DAILY, frequency = TaskFrequency.DAILY,
            defaultAssignee = Assignee.UNASSIGNED,
            points = 1, deadline = "até 30 min após o jantar", sortOrder = 6
        ),

        // ── Weekly – one per child ──────────────────────────────────────────
        Task(
            name = "Arrumar o próprio quarto — $child1Name",
            description = "Arrumar a cama, organizar roupas e itens pessoais",
            type = TaskType.WEEKLY, frequency = TaskFrequency.WEEKLY,
            defaultAssignee = Assignee.CHILD1,
            points = 3, deadline = "durante a semana", sortOrder = 7
        ),
        Task(
            name = "Arrumar o próprio quarto — $child2Name",
            description = "Arrumar a cama, organizar roupas e itens pessoais",
            type = TaskType.WEEKLY, frequency = TaskFrequency.WEEKLY,
            defaultAssignee = Assignee.CHILD2,
            points = 3, deadline = "durante a semana", sortOrder = 8
        ),
        Task(
            name = "Limpar o banheiro",
            description = "Limpar pia, vaso sanitário e passar aspirador no chão",
            type = TaskType.WEEKLY, frequency = TaskFrequency.WEEKLY,
            defaultAssignee = Assignee.UNASSIGNED,
            points = 3, deadline = "durante a semana", sortOrder = 9
        ),
    )

    private fun buildRewards() = listOf(
        Reward(name = "Escolher o filme do fim de semana", pointsCost = 5,  emoji = "🎬"),
        Reward(name = "Escolher o jantar",         pointsCost = 8,  emoji = "🍕"),
        Reward(name = "1 hora extra de tela",              pointsCost = 10, emoji = "📱"),
        Reward(name = "Recompensa especial (combinar com os pais)", pointsCost = 20, emoji = "⭐"),
    )
}
