package com.autoformfiller.app.data

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
            profileName = profile.profileName.trim()
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
    }.toString()

    private fun decode(row: ProfileEntity): Profile {
        val json = JSONObject(cipher.decrypt(row.encryptedPayload))
        return Profile(
            id = json.getString("id"),
            profileName = json.getString("profileName"),
            fullName = json.optString("fullName"),
            dateOfBirth = json.optString("dateOfBirth"),
            fatherName = json.optString("fatherName"),
            mobile = json.optString("mobile"),
            email = json.optString("email"),
            address = json.optString("address")
        )
    }
}
