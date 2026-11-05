package com.autoformfiller.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.autoformfiller.app.security.ProfileCipher

class ProfileRepository(
    private val dao: ProfileDao,
    private val cipher: ProfileCipher = ProfileCipher()
) {
    fun observeProfiles(): Flow<List<Profile>> =
        dao.observeAll().map { rows -> rows.mapNotNull { row ->
            runCatching { decode(row) }.getOrNull()
        } }

    suspend fun save(profile: Profile) {
        require(profile.profileName.isNotBlank()) { "Profile name is required." }
        val normalized = profile.copy(
            id = profile.id.ifBlank { UUID.randomUUID().toString() },
            profileName = profile.profileName.trim(),
            candidateFields = profile.candidateFields.filter { (key, value) ->
                key == value.field.key && value.value.isNotBlank()
            }
        )
        dao.upsert(
            ProfileEntity(
                id = normalized.id,
                profileName = normalized.profileName,
                encryptedPayload = cipher.encrypt(encode(normalized)),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: String) = dao.deleteById(id)

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
            profile.candidateFields.values.forEach { fieldValue ->
                put(JSONObject().apply {
                    put("fieldKey", fieldValue.field.key)
                    put("value", fieldValue.value)
                    put("sourceDocumentId", fieldValue.sourceDocumentId)
                    put("confidence", fieldValue.confidence)
                    put("status", fieldValue.status.name)
                })
            }
        })
    }.toString()

    private fun decode(row: ProfileEntity): Profile {
        val json = JSONObject(cipher.decrypt(row.encryptedPayload))
        val fields = linkedMapOf<String, CandidateFieldValue>()
        val savedFields = json.optJSONArray("candidateFields") ?: JSONArray()
        for (index in 0 until savedFields.length()) {
            val item = savedFields.optJSONObject(index) ?: continue
            val field = CandidateField.entries.firstOrNull {
                it.key == item.optString("fieldKey")
            } ?: continue
            val value = item.optString("value")
            if (value.isBlank()) continue
            val status = runCatching {
                FieldVerificationStatus.valueOf(item.optString("status"))
            }.getOrDefault(FieldVerificationStatus.EXTRACTED_UNVERIFIED)
            val confidence = if (item.isNull("confidence")) null
                else item.optDouble("confidence").toFloat().takeIf { it in 0f..1f }
            fields[field.key] = CandidateFieldValue(
                field = field,
                value = value,
                sourceDocumentId = item.optString("sourceDocumentId").takeIf {
                    it.isNotBlank() && it != "null"
                },
                confidence = confidence,
                status = status
            )
        }
        return Profile(
            id = json.getString("id"),
            profileName = json.getString("profileName"),
            fullName = json.optString("fullName"),
            dateOfBirth = json.optString("dateOfBirth"),
            fatherName = json.optString("fatherName"),
            mobile = json.optString("mobile"),
            email = json.optString("email"),
            address = json.optString("address"),
            candidateFields = fields
        )
    }
}
