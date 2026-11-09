package com.autoformfiller.app.security

import android.util.Base64
import com.autoformfiller.app.data.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable, passphrase-encrypted transfer. The passphrase is never persisted. */
object CandidateProfileTransfer {
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private val AAD = "CSC-SMART-FORM-V1".toByteArray(Charsets.UTF_8)

    fun encrypt(profile: Profile, passphrase: CharArray): String {
        require(passphrase.size >= 10) { "Use a passphrase of at least 10 characters." }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val keyBytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(encode(profile).toByteArray(Charsets.UTF_8))
        keyBytes.fill(0)
        return JSONObject().apply {
            put("format", "csc-smart-form-profile")
            put("version", 1)
            put("kdf", "PBKDF2-HMAC-SHA256")
            put("iterations", ITERATIONS)
            put("cipher", "AES-256-GCM")
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }.toString(2)
    }

    private fun encode(profile: Profile): String = JSONObject().apply {
        put("id", profile.id)
        put("profileName", profile.profileName)
        put("fullName", profile.fullName)
        put("dateOfBirth", profile.dateOfBirth)
        put("fatherName", profile.fatherName)
        put("mobile", profile.mobile)
        put("email", profile.email)
        put("address", profile.address)
        put("candidateFields", JSONArray().apply {
            profile.candidateFields.values.forEach { item ->
                put(JSONObject().apply {
                    put("fieldKey", item.field.key)
                    put("value", item.value)
                    put("status", item.status.name)
                    put("sourceDocumentId", item.sourceDocumentId)
                })
            }
        })
    }.toString()
}
