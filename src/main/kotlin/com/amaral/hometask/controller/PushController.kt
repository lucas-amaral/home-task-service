package com.amaral.hometask.controller

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.PushSubscription
import com.amaral.hometask.repository.PushSubscriptionRepository
import com.amaral.hometask.service.PushMessage
import com.amaral.hometask.service.PushResult
import com.amaral.hometask.service.PushSender
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

data class PushSubscriptionRequest(val endpoint: String, val p256dh: String, val auth: String, val assignee: Assignee)

data class PushEndpointRequest(val endpoint: String)

/** Devices subscribing to the reminders of one child. */
@RestController
@RequestMapping("/api/push")
class PushController(private val subscriptions: PushSubscriptionRepository, private val sender: PushSender) {

    /** The VAPID public key the browser needs to subscribe; empty while push is not set up. */
    @GetMapping("/public-key")
    fun publicKey() = mapOf("publicKey" to sender.publicKey, "enabled" to sender.configured)

    /** Subscribes this device to a child's reminders; subscribing again just changes the child. */
    @PostMapping("/subscriptions")
    @Transactional
    fun subscribe(@RequestBody request: PushSubscriptionRequest): Map<String, Any> {
        if (request.assignee != Assignee.CHILD1 && request.assignee != Assignee.CHILD2) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Reminders go to CHILD1 or CHILD2")
        }
        if (request.endpoint.isBlank() || request.p256dh.isBlank() || request.auth.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Incomplete subscription")
        }
        val current = subscriptions.findByEndpoint(request.endpoint)
        val saved = subscriptions.save(
            current?.copy(p256dh = request.p256dh, auth = request.auth, assignee = request.assignee)
                ?: PushSubscription(endpoint = request.endpoint, p256dh = request.p256dh, auth = request.auth, assignee = request.assignee),
        )
        return mapOf("id" to saved.id, "assignee" to saved.assignee)
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun unsubscribe(@RequestBody request: PushEndpointRequest) {
        subscriptions.findByEndpoint(request.endpoint)?.let(subscriptions::delete)
    }

    /** Sends a test notification to this device, to check it works. */
    @PostMapping("/test")
    fun test(@RequestBody request: PushEndpointRequest): Map<String, Any> {
        val device = subscriptions.findByEndpoint(request.endpoint)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "This device is not subscribed")
        val result = sender.send(device, PushMessage("Tarefas da Casa", "Notificações ativadas neste aparelho."))
        return mapOf("result" to result)
    }
}
