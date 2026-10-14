package com.amaral.hometask.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * A device (phone or browser) that receives push notifications for one child.
 * The endpoint, p256dh and auth come from the browser's PushManager.subscribe().
 */
@Entity
@Table(name = "push_subscriptions")
data class PushSubscription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 1000)
    val endpoint: String,

    @Column(nullable = false)
    val p256dh: String,

    @Column(nullable = false)
    val auth: String,

    /** CHILD1 or CHILD2: whose reminders this device gets. */
    @Enumerated(EnumType.STRING)
    val assignee: Assignee,

    val createdAt: LocalDateTime = LocalDateTime.now(),
)
