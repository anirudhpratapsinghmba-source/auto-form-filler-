package com.autoformfiller.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val profileName: String,
    /** Base64-encoded AES-GCM IV + ciphertext. Never store plaintext profile fields here. */
    val encryptedPayload: String,
    val updatedAt: Long
)
