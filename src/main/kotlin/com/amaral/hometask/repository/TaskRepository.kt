package com.amaral.hometask.repository

import com.amaral.hometask.model.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, Long> {
    fun findByActiveTrueOrderBySortOrderAsc(): List<Task>
}
