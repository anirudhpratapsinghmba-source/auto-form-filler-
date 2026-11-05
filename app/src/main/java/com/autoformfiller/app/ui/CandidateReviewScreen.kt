package com.autoformfiller.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoformfiller.app.data.CandidateField
import com.autoformfiller.app.data.CandidateFieldValue
import com.autoformfiller.app.data.FieldVerificationStatus
import com.autoformfiller.app.data.Profile
import com.autoformfiller.app.ocr.DocumentOcr
import java.util.UUID

@Composable
fun CandidateReviewScreen(modifier: Modifier = Modifier, onSave: (Profile) -> Unit) {
    val context = LocalContext.current
    val values = remember { mutableStateMapOf<String, String>() }
    val confirmed = remember { mutableStateMapOf<String, Boolean>() }
    val conflicts = remember { mutableStateMapOf<String, String>() }
    var rawText by remember { mutableStateOf("") }
    var sourceDocumentId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Choose a PDF or document image. Processing stays on this device.") }
    var busy by remember { mutableStateOf(false) }

    fun acceptOcrText(text: String, documentId: String) {
        rawText = text
        sourceDocumentId = documentId
        val extracted = CandidateTextParser.parse(text)
        var conflictCount = 0
        extracted.forEach { (key, value) ->
            val old = values[key].orEmpty()
            if (old.isNotBlank() && !old.equals(value, ignoreCase = true)) {
                conflicts[key] = old
                conflictCount++
            } else {
                values[key] = value
                conflicts.remove(key)
                confirmed[key] = false
            }
        }
        status = when {
            text.isBlank() -> "No readable text detected. Try a clearer document."
            conflictCount > 0 -> "$conflictCount conflicting field(s) found. Compare with the original document and resolve them manually."
            else -> "Text extracted. All OCR values are unverified until you confirm them."
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            status = "Reading image locally…"
            val documentId = UUID.randomUUID().toString()
            DocumentOcr.read(context, uri) { result ->
                busy = false
                result.onSuccess { text -> acceptOcrText(text, documentId) }
                    .onFailure { status = "Could not read this image. Try a clear JPG or PNG." }
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            status = "Reading PDF locally…"
            val documentId = UUID.randomUUID().toString()
            DocumentOcr.readPdf(context, uri) { result ->
                busy = false
                result.onSuccess { text -> acceptOcrText(text, documentId) }
                    .onFailure { status = "Could not read this PDF. Try a smaller, non-password-protected PDF." }
            }
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DOCUMENT IMPORT", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        Text("Select → Extract → Review", style = MaterialTheme.typography.headlineMedium)
        Text("PDF and image OCR run on-device. Nothing is uploaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Choose PDF document")
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Choose image (JPG / PNG)") }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        HorizontalDivider()
        Text("CANDIDATE MASTER PROFILE", style = MaterialTheme.typography.titleSmall)
        Text(
            "Review each value. Tick Confirm only after checking it against the original document. Conflicts must be resolved before saving.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        CandidateField.entries.forEach { field ->
            val value = values[field.key].orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { values[field.key] = it },
                    label = { Text(field.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = field != CandidateField.ADDRESS_LINE
                )
                conflicts[field.key]?.let { previous ->
                    Text(
                        "Conflict: earlier document/value was “$previous”. Verify the correct value before saving.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                if (value.isNotBlank()) {
                    Row {
                        Checkbox(
                            checked = confirmed[field.key] == true,
                            onCheckedChange = { confirmed[field.key] = it },
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "I verified this value",
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (confirmed[field.key] == true) "Confirmed" else "Unverified",
                            color = if (confirmed[field.key] == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 10.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val candidateName = values[CandidateField.FULL_NAME.key].orEmpty().trim()
                val fieldValues = CandidateField.entries.mapNotNull { field ->
                    val value = values[field.key]?.trim().orEmpty()
                    if (value.isBlank()) null else field.key to CandidateFieldValue(
                        field = field,
                        value = value,
                        sourceDocumentId = sourceDocumentId,
                        confidence = null,
                        status = if (confirmed[field.key] == true) {
                            FieldVerificationStatus.OPERATOR_CONFIRMED
                        } else {
                            FieldVerificationStatus.EXTRACTED_UNVERIFIED
                        }
                    )
                }.toMap()
                val unresolved = fieldValues.keys.any { it in conflicts.keys }
                if (unresolved) {
                    status = "Resolve every conflicting field before saving."
                } else if (fieldValues.isEmpty()) {
                    status = "Add at least one candidate detail before saving."
                } else {
                    onSave(
                        Profile(
                            id = UUID.randomUUID().toString(),
                            profileName = candidateName.ifBlank { "New candidate" },
                            fullName = candidateName,
                            dateOfBirth = values[CandidateField.DATE_OF_BIRTH.key].orEmpty(),
                            fatherName = values[CandidateField.FATHER_NAME.key].orEmpty(),
                            mobile = values[CandidateField.MOBILE.key].orEmpty(),
                            email = values[CandidateField.EMAIL.key].orEmpty(),
                            address = values[CandidateField.ADDRESS_LINE.key].orEmpty(),
                            candidateFields = fieldValues
                        )
                    )
                    status = "Candidate profile saved. Unconfirmed values remain marked unverified."
                }
            },
            enabled = !busy && values.values.any { it.isNotBlank() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save reviewed candidate") }

        if (rawText.isNotBlank()) {
            HorizontalDivider()
            Text("EXTRACTED SOURCE TEXT", style = MaterialTheme.typography.titleSmall)
            Text(rawText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("OCR may make mistakes. Confirm every saved value against its original document.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Conservative label-based extraction: no value is inferred from an unlabeled number or line. */
private object CandidateTextParser {
    fun parse(text: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            val separator = listOf(':', '：').map { line.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return@forEach
            if (separator == 0 || separator >= line.lastIndex) return@forEach
            val label = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim().take(2000)
            if (value.isBlank()) return@forEach
            val field = CandidateField.findByAlias(label) ?: return@forEach
            result.putIfAbsent(field.key, value)
        }
        return result
    }
}
