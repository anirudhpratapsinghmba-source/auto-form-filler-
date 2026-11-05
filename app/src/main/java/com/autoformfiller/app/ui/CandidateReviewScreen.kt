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
import com.autoformfiller.app.data.Profile
import com.autoformfiller.app.ocr.DocumentOcr
import java.util.UUID

@Composable
fun CandidateReviewScreen(modifier: Modifier = Modifier, onSave: (Profile) -> Unit) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Choose a document image to extract text on this device.") }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            status = "Reading document locally…"
            DocumentOcr.read(context, uri) { result ->
                busy = false
                result.onSuccess { text ->
                    rawText = text
                    val parsed = CandidateTextParser.parse(text)
                    fullName = parsed.name.ifBlank { fullName }
                    dob = parsed.dob.ifBlank { dob }
                    fatherName = parsed.father.ifBlank { fatherName }
                    mobile = parsed.mobile.ifBlank { mobile }
                    email = parsed.email.ifBlank { email }
                    address = parsed.address.ifBlank { address }
                    status = if (text.isBlank()) "No text detected. Try a clearer, well-lit image." else "Text extracted. Please verify and correct every field before saving."
                }.onFailure {
                    status = "Could not read this image. Try another clear JPG or PNG."
                }
            }
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DOCUMENT IMPORT", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        Text("Select → Extract → Review", style = MaterialTheme.typography.headlineMedium)
        Text("Choose a document photo from this device. OCR runs on-device; no image is uploaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Reading…" else "Choose document image")
        }
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        HorizontalDivider()
        Text("REVIEW CANDIDATE DETAILS", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dob, { dob = it }, label = { Text("Date of birth") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(fatherName, { fatherName = it }, label = { Text("Father's name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(
            onClick = {
                val candidateName = fullName.trim()
                onSave(Profile(UUID.randomUUID().toString(), candidateName.ifBlank { "New candidate" }, candidateName, dob.trim(), fatherName.trim(), mobile.trim(), email.trim(), address.trim()))
            },
            enabled = !busy && listOf(fullName, dob, fatherName, mobile, email, address).any { it.isNotBlank() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save reviewed candidate") }
        if (rawText.isNotBlank()) {
            HorizontalDivider()
            Text("EXTRACTED SOURCE TEXT", style = MaterialTheme.typography.titleSmall)
            Text(rawText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("OCR can make mistakes. Confirm details against the original document.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

private data class ParsedCandidate(val name: String = "", val dob: String = "", val father: String = "", val mobile: String = "", val email: String = "", val address: String = "")

private object CandidateTextParser {
    fun parse(text: String): ParsedCandidate {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        fun labelled(pattern: String): String {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            return lines.firstNotNullOfOrNull { line -> regex.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } }.orEmpty()
        }
        val mobile = Regex("(?<!\\d)[6-9]\\d{9}(?!\\d)").find(text)?.value.orEmpty()
        val email = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE).find(text)?.value.orEmpty()
        val dob = labelled("^(?:date of birth|dob| जन्म तिथि|जन्मतिथि)\\s*[:：-]?\\s*(.+)$")
            .ifBlank { Regex("\\b\\d{2}[/-]\\d{2}[/-]\\d{4}\\b").find(text)?.value.orEmpty() }
        return ParsedCandidate(
            name = labelled("^(?:candidate name|applicant name|student name|name)\\s*[:：-]?\\s*(.+)$"),
            dob = dob,
            father = labelled("^(?:father'?s name|father name|पिता का नाम)\\s*[:：-]?\\s*(.+)$"),
            mobile = mobile,
            email = email,
            address = labelled("^(?:address|पता)\\s*[:：-]?\\s*(.+)$")
        )
    }
}
