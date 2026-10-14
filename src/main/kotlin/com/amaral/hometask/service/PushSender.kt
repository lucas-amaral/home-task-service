package com.amaral.hometask.service

import com.amaral.hometask.model.PushSubscription
import com.fasterxml.jackson.databind.ObjectMapper
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Security

/** What a push notification shows: title, text and the page it opens when tapped. */
data class PushMessage(val title: String, val body: String, val url: String = "/")

/** How a push to one device went. */
enum class PushResult { SENT, GONE, FAILED }

/** Delivers a push notification to one device. */
interface PushSender {
    /** False while the VAPID keys are not set, in which case nothing can be sent. */
    val configured: Boolean
    val publicKey: String
    fun send(subscription: PushSubscription, message: PushMessage): PushResult
}

/**
 * Web Push with VAPID keys (VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT).
 * Generate the keys once with `./gradlew generateVapidKeys` and keep them: changing them
 * invalidates every device already subscribed.
 */
@Component
class WebPushSender(
    @Value("\${push.vapid.public-key:}") override val publicKey: String,
    @Value("\${push.vapid.private-key:}") private val privateKey: String,
    @Value("\${push.vapid.subject:mailto:admin@example.com}") private val subject: String,
    private val mapper: ObjectMapper,
) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override val configured: Boolean get() = publicKey.isNotBlank() && privateKey.isNotBlank()

    private val service: PushService by lazy {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider())
        PushService(publicKey, privateKey, subject)
    }

    override fun send(subscription: PushSubscription, message: PushMessage): PushResult {
        if (!configured) return PushResult.FAILED
        return try {
            val payload = mapper.writeValueAsBytes(message)
            val response = service.send(Notification(subscription.endpoint, subscription.p256dh, subscription.auth, payload))
            when (val status = response.statusLine.statusCode) {
                in 200..299 -> PushResult.SENT
                // The browser dropped this subscription (app uninstalled, permission revoked).
                404, 410 -> PushResult.GONE
                else -> {
                    log.warn("Push to {} answered {}", subscription.id, status)
                    PushResult.FAILED
                }
            }
        } catch (ex: Exception) {
            log.warn("Push to {} failed: {}", subscription.id, ex.message)
            PushResult.FAILED
        }
    }
}
