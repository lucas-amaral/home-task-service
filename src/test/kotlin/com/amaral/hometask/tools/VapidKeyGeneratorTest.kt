package com.amaral.hometask.tools

import nl.martijndwars.webpush.Encoding
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class VapidKeyGeneratorTest {

    private val encoder = Base64.getUrlEncoder().withoutPadding()

    @Test
    fun `the keys have the sizes browsers expect`() {
        val (public, private) = generateVapidKeys()

        assertEquals(65, Base64.getUrlDecoder().decode(public).size)
        assertEquals(32, Base64.getUrlDecoder().decode(private).size)
    }

    @Test
    fun `the push library signs and encrypts a notification with them, for a browser-like subscription`() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(BouncyCastleProvider())
        val (public, private) = generateVapidKeys()
        // What a browser hands over on subscribe: its own P-256 public key and 16 random bytes.
        val browser = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val point = (browser.public as ECPublicKey).w
        val p256dh = encoder.encodeToString(byteArrayOf(4) + point.affineX.fixed() + point.affineY.fixed())
        val auth = encoder.encodeToString(ByteArray(16).also(SecureRandom()::nextBytes))

        val post = PushService(public, private, "mailto:test@example.com")
            .preparePost(Notification("https://push.example.com/abc", p256dh, auth, "{}".toByteArray()), Encoding.AES128GCM)

        assertNotNull(post.getFirstHeader("Authorization"))
        assertEquals("aes128gcm", post.getFirstHeader("Content-Encoding").value)
    }

    private fun java.math.BigInteger.fixed(): ByteArray {
        val bytes = toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        return ByteArray(32 - bytes.size) + bytes
    }
}
