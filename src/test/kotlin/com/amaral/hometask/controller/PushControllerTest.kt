package com.amaral.hometask.controller

import com.amaral.hometask.model.Assignee
import com.amaral.hometask.model.PushSubscription
import com.amaral.hometask.repository.PushSubscriptionRepository
import com.amaral.hometask.service.PushResult
import com.amaral.hometask.service.PushSender
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(PushController::class)
@ActiveProfiles("test")
class PushControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockBean lateinit var subscriptions: PushSubscriptionRepository
    @MockBean lateinit var sender: PushSender

    private val body = """{"endpoint":"https://push/1","p256dh":"key","auth":"secret","assignee":"CHILD1"}"""

    @Test
    fun `the public key says whether push is on`() {
        whenever(sender.publicKey).thenReturn("BPublic")
        whenever(sender.configured).thenReturn(true)

        mvc.get("/api/push/public-key").andExpect {
            status { isOk() }
            jsonPath("$.publicKey") { value("BPublic") }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `a device subscribes to a child, and subscribing again changes the child instead of duplicating`() {
        whenever(subscriptions.save(any<PushSubscription>())).thenAnswer { (it.arguments[0] as PushSubscription).copy(id = 5) }

        mvc.post("/api/push/subscriptions") { contentType = MediaType.APPLICATION_JSON; content = body }
            .andExpect { status { isOk() }; jsonPath("$.assignee") { value("CHILD1") } }
        verify(subscriptions).save(argThat<PushSubscription> { id == 0L && endpoint == "https://push/1" && assignee == Assignee.CHILD1 })

        whenever(subscriptions.findByEndpoint("https://push/1"))
            .thenReturn(PushSubscription(id = 5, endpoint = "https://push/1", p256dh = "key", auth = "secret", assignee = Assignee.CHILD1))
        mvc.post("/api/push/subscriptions") { contentType = MediaType.APPLICATION_JSON; content = body.replace("CHILD1", "CHILD2") }
            .andExpect { status { isOk() } }
        verify(subscriptions).save(argThat<PushSubscription> { id == 5L && assignee == Assignee.CHILD2 })
    }

    @Test
    fun `reminders only go to one of the children`() {
        mvc.post("/api/push/subscriptions") { contentType = MediaType.APPLICATION_JSON; content = body.replace("CHILD1", "BOTH") }
            .andExpect { status { isBadRequest() } }
        verify(subscriptions, never()).save(any<PushSubscription>())
    }

    @Test
    fun `a device unsubscribes, and can ask for a test notification while subscribed`() {
        val device = PushSubscription(id = 5, endpoint = "https://push/1", p256dh = "key", auth = "secret", assignee = Assignee.CHILD1)
        whenever(subscriptions.findByEndpoint("https://push/1")).thenReturn(device)
        whenever(sender.send(any(), any())).thenReturn(PushResult.SENT)

        mvc.post("/api/push/test") { contentType = MediaType.APPLICATION_JSON; content = """{"endpoint":"https://push/1"}""" }
            .andExpect { status { isOk() }; jsonPath("$.result") { value("SENT") } }
        mvc.delete("/api/push/subscriptions") { contentType = MediaType.APPLICATION_JSON; content = """{"endpoint":"https://push/1"}""" }
            .andExpect { status { isNoContent() } }
        verify(subscriptions).delete(device)
        mvc.post("/api/push/test") { contentType = MediaType.APPLICATION_JSON; content = """{"endpoint":"https://push/2"}""" }
            .andExpect { status { isNotFound() } }
    }
}
