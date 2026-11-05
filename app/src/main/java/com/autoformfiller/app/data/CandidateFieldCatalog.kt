package com.autoformfiller.app.data

/** Canonical field vocabulary shared by document extraction and portal adapters. */
enum class CandidateField(val key: String, val displayName: String, val aliases: List<String>, val sensitive: Boolean = false) {
    FULL_NAME("fullName", "Candidate full name", listOf("candidate name", "applicant name", "student name", "अभ्यर्थी का नाम", "आवेदक का नाम")),
    NAME_AS_PER_10TH("nameAsPer10th", "Name as per 10th certificate", listOf("name in matriculation certificate")),
    FATHER_NAME("fatherName", "Father's name", listOf("father name", "पिता का नाम")),
    MOTHER_NAME("motherName", "Mother's name", listOf("mother name", "माता का नाम")),
    DATE_OF_BIRTH("dateOfBirth", "Date of birth", listOf("dob", "birth date", "जन्म तिथि")),
    GENDER("gender", "Gender", listOf("sex", "लिंग")),
    CATEGORY("category", "Category", listOf("reservation category", "सामाजिक वर्ग")),
    NATIONALITY("nationality", "Nationality", listOf("राष्ट्रीयता")),
    MARITAL_STATUS("maritalStatus", "Marital status", listOf("वैवाहिक स्थिति")),
    MOBILE("mobile", "Mobile number", listOf("phone", "contact number", "मोबाइल नंबर")),
    EMAIL("email", "Email address", listOf("e-mail", "ईमेल")),
    ADDRESS_LINE("addressLine", "Address", listOf("residential address", "पता")),
    STATE("state", "State", listOf("राज्य")),
    DISTRICT("district", "District", listOf("जिला")),
    PIN_CODE("pinCode", "PIN code", listOf("postal code", "पिन कोड")),
    DOMICILE_STATE("domicileState", "Domicile state", listOf("state of domicile", "मूल निवास राज्य")),
    DISABILITY_STATUS("disabilityStatus", "Disability status", listOf("benchmark disability", "दिव्यांगता")),
    DISABILITY_PERCENTAGE("disabilityPercentage", "Disability percentage", listOf("दिव्यांगता प्रतिशत")),
    BOARD_10TH("board10th", "10th board", listOf("matric board", "secondary board")),
    ROLL_10TH("roll10th", "10th roll number", listOf("matric roll number")),
    PASSING_YEAR_10TH("passingYear10th", "10th passing year", listOf("matric year")),
    MARKS_10TH("marks10th", "10th marks", listOf("secondary marks")),
    BOARD_12TH("board12th", "12th board", listOf("senior secondary board")),
    ROLL_12TH("roll12th", "12th roll number", listOf("intermediate roll number")),
    PASSING_YEAR_12TH("passingYear12th", "12th passing year", listOf("intermediate passing year")),
    STREAM_12TH("stream12th", "12th stream", listOf("subject stream")),
    MARKS_12TH("marks12th", "12th marks", listOf("senior secondary marks")),
    GRADUATION_UNIVERSITY("graduationUniversity", "Graduation university", listOf("university name")),
    GRADUATION_COURSE("graduationCourse", "Graduation course", listOf("degree", "qualification")),
    GRADUATION_PASSING_YEAR("graduationPassingYear", "Graduation passing year", listOf("degree passing year")),
    GRADUATION_PERCENTAGE("graduationPercentage", "Graduation percentage", listOf("cgpa", "degree marks")),
    PHOTO_DOCUMENT("photoDocument", "Candidate photograph", listOf("passport photo", "photograph")),
    SIGNATURE_DOCUMENT("signatureDocument", "Candidate signature", listOf("signature")),
    CASTE_CERTIFICATE_NUMBER("casteCertificateNumber", "Caste certificate number", listOf("category certificate number"), true),
    DOMICILE_CERTIFICATE_NUMBER("domicileCertificateNumber", "Domicile certificate number", listOf("residence certificate number"), true),
    IDENTITY_NUMBER("identityNumber", "Identity document number", listOf("id number"), true);

    companion object {
        fun findByAlias(label: String): CandidateField? {
            val normalized = normalize(label)
            return entries.firstOrNull { field ->
                (listOf(field.displayName, field.key) + field.aliases).any { normalize(it) == normalized }
            }
        }
        private fun normalize(value: String) = value.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
    }
}

enum class CandidateDocumentType { UNKNOWN, CLASS_10_MARKSHEET, CLASS_12_MARKSHEET, GRADUATION_MARKSHEET, PHOTO, SIGNATURE, CASTE_CERTIFICATE, DOMICILE_CERTIFICATE, IDENTITY_DOCUMENT, DISABILITY_CERTIFICATE, OTHER }
enum class FieldVerificationStatus { EXTRACTED_UNVERIFIED, OPERATOR_CONFIRMED, CONFLICT, MISSING, INVALID }

data class CandidateFieldValue(
    val field: CandidateField,
    val value: String,
    val sourceDocumentId: String?,
    val confidence: Float?,
    val status: FieldVerificationStatus = FieldVerificationStatus.EXTRACTED_UNVERIFIED
) {
    init {
        require(value.length <= 2000) { "Candidate field value is too long." }
        require(confidence == null || confidence in 0f..1f) { "Confidence must be between 0 and 1." }
    }
}
