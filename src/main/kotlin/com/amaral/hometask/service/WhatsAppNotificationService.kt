package com.amaral.hometask.service

import com.amaral.hometask.model.Assignment
import com.amaral.hometask.model.Assignee
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * Sends WhatsApp messages via the Twilio WhatsApp API (sandbox or production).
 *
 * Required env vars:
 *   TWILIO_ACCOUNT_SID  — your Twilio Account SID
 *   TWILIO_AUTH_TOKEN   — your Twilio Auth Token
 *   TWILIO_WA_FROM      — sender number, e.g. "whatsapp:+14155238886"
 *   WHATSAPP_CHILD1     — recipient for child 1, e.g. "whatsapp:+5511999999991"
 *   WHATSAPP_CHILD2     — recipient for child 2, e.g. "whatsapp:+5511999999992"
 *   WHATSAPP_PARENTS    — recipient for parents (optional fallback)
 *
 * If any required variable is blank the service logs a warning and skips sending.
 */
@Service
class WhatsAppNotificationService(
    @Value("\${twilio.account-sid:}") private val accountSid: String,
    @Value("\${twilio.auth-token:}") private val authToken: String,
    @Value("\${twilio.whatsapp.from:}") private val from: String,
    @Value("\${whatsapp.child1:}") private val child1Number: String,
    @Value("\${whatsapp.child2:}") private val child2Number: String,
    @Value("\${whatsapp.parents:}") private val parentsNumber: String,
    @Value("\${app.family.child1-name:Child 1}") private val child1Name: String,
    @Value("\${app.family.child2-name:Child 2}") private val child2Name: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val twilioApiUrl
        get() = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"

    fun sendDeadlineMissed(assignment: Assignment) {
        if (!isConfigured()) {
            log.warn(
                "WhatsApp not configured — skipping notification for assignment {} ({})",
                assignment.id, assignment.task.name
            )
            return
        }

        val recipients = resolveRecipients(assignment.assignedTo)
        val message = buildMessage(assignment)

        recipients.forEach { to ->
            sendMessage(to, message)
        }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun isConfigured() =
        accountSid.isNotBlank() && authToken.isNotBlank() && from.isNotBlank()

    private fun resolveRecipients(assignee: Assignee): List<String> = when (assignee) {
        Assignee.CHILD1 -> listOfNotNull(child1Number.blankToNull())
        Assignee.CHILD2 -> listOfNotNull(child2Number.blankToNull())
        Assignee.BOTH   -> listOfNotNull(child1Number.blankToNull(), child2Number.blankToNull())
        Assignee.UNASSIGNED -> listOfNotNull(parentsNumber.blankToNull())
    }

    private fun buildMessage(assignment: Assignment): String {
        val childName = when (assignment.assignedTo) {
            Assignee.CHILD1     -> child1Name
            Assignee.CHILD2     -> child2Name
            Assignee.BOTH       -> "$child1Name e $child2Name"
            Assignee.UNASSIGNED -> "Alguém"
        }
        val deadline = assignment.task.deadline.ifBlank { "hoje" }
        return "⏰ *Tarefa não concluída!*\n" +
               "Olá $childName, a tarefa *${assignment.task.name}* " +
               "deveria ter sido feita até às $deadline e ainda não foi registrada.\n" +
               "Não esqueça! 💪"
    }

    private fun sendMessage(to: String, body: String) {
        try {
            val client = RestClient.builder()
                .baseUrl(twilioApiUrl)
                .defaultHeaders { it.setBasicAuth(accountSid, authToken) }
                .build()

            val response = client.post()
                .body("To=$to&From=$from&Body=${java.net.URLEncoder.encode(body, "UTF-8")}")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .toBodilessEntity()

            log.info("WhatsApp sent to {} — HTTP {}", to, response.statusCode)
        } catch (ex: Exception) {
            log.error("WhatsApp send failed to {}: {}", to, ex.message)
            throw ex
        }
    }

    private fun String.blankToNull() = ifBlank { null }
}
