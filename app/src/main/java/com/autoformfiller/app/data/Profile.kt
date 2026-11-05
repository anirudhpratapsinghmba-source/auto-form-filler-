package com.autoformfiller.app.data

/**
 * Plaintext profile model. Only encrypted payloads are persisted to Room.
 * Do not add passwords, OTPs, or payment credentials to this model.
 */
data class Profile(
    val id: String,
    val profileName: String,
    val fullName: String = "",
    val dateOfBirth: String = "",
    val fatherName: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    /** Additional exam/document fields keyed by CandidateField.key. */
    val candidateFields: Map<String, CandidateFieldValue> = emptyMap()
)
