package com.autoformfiller.app.security

import com.autoformfiller.app.data.CandidateField
import com.autoformfiller.app.data.FieldVerificationStatus
import com.autoformfiller.app.data.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Interoperable, passphrase-protected export for the Chrome extension.
 * The passphrase is never persisted. The export contains no database key.
 */
object EncryptedProfilePackage {
    private const val FORMAT = "csc-smart-profile"
    private const val VERSION = 1
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256

    fun encrypt(profile: Profile, passphrase: CharArray): String {
        require(passphrase.size in 12..128) { "Use a passphrase between 12 and 128 characters." }

        val payload = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("profileName", profile.profileName)
            put("fields", JSONArray().apply {
                val existing = profile.candidateFields
                CandidateField.entries.forEach { field ->
                    val stored = existing[field.key]
                    val value = stored?.value?.takeIf { it.isNotBlank() } ?: legacyValue(profile, field)
                    if (!value.isNullOrBlank()) {
                        put(JSONObject().apply {
                            put("key", field.key)
                            put("label", field.displayName)
                            put("value", value)
                            put("status", stored?.status?.name ?: FieldVerificationStatus.EXTRACTED_UNVERIFIED.name)
                            put("sensitive", field.sensitive)
                        })
                    }
                }
            })
        }.toString().toByteArray(Charsets.UTF_8)

        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = deriveKey(passphrase, salt)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val ciphertext = cipher.doFinal(payload)
            JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("kdf", "PBKDF2-HMAC-SHA256")
                put("iterations", ITERATIONS)
                put("cipher", "AES-256-GCM")
                put("salt", encode(salt))
                put("iv", encode(iv))
                put("ciphertext", encode(ciphertext))
            }.toString()
        } finally {
            payload.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try {
                SecretKeySpec(bytes, "AES")
            } finally {
                bytes.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun legacyValue(profile: Profile, field: CandidateField): String? = when (field) {
        CandidateField.FULL_NAME -> profile.fullName
        CandidateField.DATE_OF_BIRTH -> profile.dateOfBirth
        CandidateField.FATHER_NAME -> profile.fatherName
        CandidateField.MOBILE -> profile.mobile
        CandidateField.EMAIL -> profile.email
        CandidateField.ADDRESS_LINE -> profile.address
        else -> null
    }

    private fun String?.isNullOrBlank(): Boolean = this == null || this.isBlank()
}
