package com.amaral.hometask.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Sends WhatsApp messages via Callmebot — a free, no-registration service.
 *
 * ## Setup (one-time per phone number)
 * 1. Save the Callmebot number in WhatsApp contacts:
 *    +34 644 61 79 98  (or check https://www.callmebot.com/blog/free-api-whatsapp-messages/)
 * 2. Send the message "I allow callmebot to send me messages" to that number.
 * 3. You will receive your personal apikey in reply.
 * 4. Store the phone + apikey pair in FamilyConfig (child1Phone / child2Phone)
 *    in the format "5554999990000:APIKEY" (phone:apikey).
 *
 * No paid plan, no server, no OAuth — just a GET request.
 */
@Service
class WhatsAppNotifier {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Sends a WhatsApp message.
     * @param phoneWithKey  "5554999990000:APIKEY" — phone number + Callmebot API key
     * @param message       Plain text message (max ~1600 chars)
     * @return true if the HTTP call succeeded (2xx), false otherwise
     */
    fun send(phoneWithKey: String, message: String): Boolean {
        if (phoneWithKey.isBlank()) return false

        val parts = phoneWithKey.split(":")
        if (parts.size != 2) {
            log.warn("WhatsApp: invalid phone format — expected 'phone:apikey', got '{}'", phoneWithKey)
            return false
        }

        val phone  = parts[0].trim()
        val apiKey = parts[1].trim()
        if (phone.isBlank() || apiKey.isBlank()) return false

        val encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8)
        val url = "https://api.callmebot.com/whatsapp.php?phone=$phone&text=$encodedMsg&apikey=$apiKey"

        return try {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout    = 8_000
            conn.setRequestProperty("User-Agent", "HomeTaskService/1.0")
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                log.info("WhatsApp notification sent to {} (HTTP {})", phone, code)
                true
            } else {
                log.warn("WhatsApp notification failed for {} — HTTP {}", phone, code)
                false
            }
        } catch (ex: Exception) {
            log.error("WhatsApp notification error for {}: {}", phone, ex.message)
            false
        }
    }
}
