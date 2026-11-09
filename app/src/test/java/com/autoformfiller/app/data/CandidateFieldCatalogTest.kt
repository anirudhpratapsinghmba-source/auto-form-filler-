package com.autoformfiller.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateFieldCatalogTest {
    @Test
    fun everyFieldKeyIsUniqueAndNonBlank() {
        val keys = CandidateField.entries.map { it.key }
        assertTrue(keys.all { it.isNotBlank() })
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun commonEnglishAndHindiLabelsResolveToCanonicalFields() {
        assertEquals(CandidateField.FULL_NAME, CandidateField.findByAlias("Candidate Name"))
        assertEquals(CandidateField.DATE_OF_BIRTH, CandidateField.findByAlias("DOB"))
        assertEquals(CandidateField.FATHER_NAME, CandidateField.findByAlias("पिता का नाम"))
        assertEquals(CandidateField.MOTHER_NAME, CandidateField.findByAlias("माता का नाम"))
    }

    @Test
    fun sensitiveCertificateAndIdentityFieldsAreExplicitlyMarked() {
        assertTrue(CandidateField.CASTE_CERTIFICATE_NUMBER.sensitive)
        assertTrue(CandidateField.DOMICILE_CERTIFICATE_NUMBER.sensitive)
        assertTrue(CandidateField.IDENTITY_NUMBER.sensitive)
        assertFalse(CandidateField.FULL_NAME.sensitive)
    }

    @Test
    fun unknownLabelsAreNotGuessed() {
        assertEquals(null, CandidateField.findByAlias("Random Number"))
        assertNotNull(CandidateField.findByAlias("10th roll number"))
    }
}
