package com.amaral.hometask.repository

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.PushSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PushSubscriptionRepository : JpaRepository<PushSubscription, Long> {
    fun findByEndpoint(endpoint: String): PushSubscription?
    fun findByAssignee(assignee: Assignee): List<PushSubscription>
}
