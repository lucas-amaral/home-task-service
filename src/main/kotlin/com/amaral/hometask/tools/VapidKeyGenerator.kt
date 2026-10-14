package com.amaral.hometask.tools

import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * Prints a new VAPID key pair (P-256), in the base64url form browsers and the push library expect.
 * Run once with `./gradlew generateVapidKeys` and keep the keys as VAPID_PUBLIC_KEY and
 * VAPID_PRIVATE_KEY: new keys make every device already subscribed stop receiving.
 */
fun main() {
    val (public, private) = generateVapidKeys()
    println("VAPID_PUBLIC_KEY=$public")
    println("VAPID_PRIVATE_KEY=$private")
}

/** A new VAPID key pair: (public, private), both base64url without padding. */
fun generateVapidKeys(): Pair<String, String> {
    val generator = KeyPairGenerator.getInstance("EC")
    generator.initialize(ECGenParameterSpec("secp256r1"))
    val pair = generator.generateKeyPair()
    val public = pair.public as ECPublicKey
    val private = pair.private as ECPrivateKey
    // Uncompressed point: 0x04 followed by X and Y, 32 bytes each.
    val point = byteArrayOf(4) + public.w.affineX.toFixed(32) + public.w.affineY.toFixed(32)
    val encoder = Base64.getUrlEncoder().withoutPadding()
    return encoder.encodeToString(point) to encoder.encodeToString(private.s.toFixed(32))
}

private fun java.math.BigInteger.toFixed(size: Int): ByteArray {
    val bytes = toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
    return ByteArray(size - bytes.size) + bytes
}
