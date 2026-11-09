package com.autoformfiller.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.autoformfiller.app.data.AppDatabase
import com.autoformfiller.app.data.Profile
import com.autoformfiller.app.data.ProfileRepository
import com.autoformfiller.app.ui.CandidateReviewScreen
import com.autoformfiller.app.security.EncryptedProfilePackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Ink = Color(0xFF14213D)
private val Accent = Color(0xFF2563EB)
private val Canvas = Color(0xFFF5F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AutoFormFillerApp() }
    }
}

@Composable
private fun AutoFormFillerApp() {
    val context = LocalContext.current
    val repository = remember { ProfileRepository(AppDatabase.getInstance(context).profileDao()) }
    val scope = rememberCoroutineScope()
    val profiles by repository.observeProfiles().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    var exportTarget by remember { mutableStateOf<Profile?>(null) }
    var exportPassphrase by remember { mutableStateOf<CharArray?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val target = exportTarget
        val password = exportPassphrase
        if (uri != null && target != null && password != null) {
            scope.launch {
                try {
                    val encrypted = withContext(Dispatchers.Default) {
                        EncryptedProfilePackage.encrypt(target, password)
                    }
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri, "w")
                            ?: error("Could not create the export file.")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(encrypted) }
                    }
                    Toast.makeText(context, "Encrypted profile exported. Import it in the Chrome extension.", Toast.LENGTH_LONG).show()
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Profile export failed.", Toast.LENGTH_LONG).show()
                } finally {
                    password.fill('\u0000')
                    exportTarget = null
                    exportPassphrase = null
                }
            }
        } else {
            password?.fill('\u0000')
            exportTarget = null
            exportPassphrase = null
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Canvas)) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profiles") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Keyboard, null) }, label = { Text("Keyboard") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Description, null) }, label = { Text("Documents") })
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) FloatingActionButton(
                    onClick = { editingProfile = null; showEditor = true },
                    containerColor = Accent, contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "Add profile") }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> ProfilesScreen(
                    profiles = profiles,
                    modifier = Modifier.padding(padding),
                    onAdd = { editingProfile = null; showEditor = true },
                    onEdit = { editingProfile = it; showEditor = true },
                    onDelete = { profile -> scope.launch { repository.delete(profile.id) } },
                    onExport = { profile -> exportTarget = profile }
                )
                1 -> FeaturePlaceholder("Smart Keyboard", "Android keyboard insertion is the next Android milestone. It will insert only the candidate field you choose into supported fields; no clipboard dependency and no keystroke logging.", Modifier.padding(padding))
                else -> CandidateReviewScreen(
                    modifier = Modifier.padding(padding),
                    onSave = { profile -> scope.launch { repository.save(profile) }; selectedTab = 0 }
                )
            }
        }
        exportTarget?.let { target ->
            ExportProfileDialog(
                profile = target,
                onDismiss = { exportTarget = null },
                onExport = { phrase ->
                    exportPassphrase = phrase.toCharArray()
                    val safeName = target.profileName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40).ifBlank { "candidate" }
                    exportLauncher.launch("$safeName.cscprofile")
                }
            )
        }
        if (showEditor) {
            ProfileEditorDialog(
                existing = editingProfile,
                onDismiss = { showEditor = false },
                onSave = { profile -> scope.launch { repository.save(profile) }; showEditor = false }
            )
        }
    }
}

@Composable
private fun ProfilesScreen(
    profiles: List<Profile>,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onExport: (Profile) -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)
    ) {
        item {
            Text("CSC SMART FORM FILLER", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Candidate details.\nReady for review.", color = Ink, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Create separate profiles for each candidate. Personal data stays encrypted on this device.", color = Color(0xFF64748B), fontSize = 14.sp)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("CANDIDATE WORKSPACE", color = Color(0xFFB9CCF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("${profiles.size}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("Saved candidate profiles", color = Color(0xFFD5DEEF), fontSize = 13.sp)
                }
            }
        }
        item { Text("SAVED CANDIDATES", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
        if (profiles.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, null, tint = Accent, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No candidates yet", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Add a candidate or import a document.", color = Color(0xFF64748B), fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onAdd) { Text("Create candidate") }
                    }
                }
            }
        }
        items(profiles, key = { it.id }) { profile ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFEAF0FF), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Accent) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile.profileName, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(profile.fullName.ifBlank { "Candidate profile" }, color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Text("LOCAL", color = Color(0xFF16835D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    val summary = listOf(profile.mobile, profile.email, profile.address).filter { it.isNotBlank() }
                    if (summary.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(summary.joinToString("  ·  "), color = Color(0xFF64748B), fontSize = 12.sp, maxLines = 2)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onExport(profile) }) { Text("Export") }
                        TextButton(onClick = { onEdit(profile) }) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit") }
                        TextButton(onClick = { onDelete(profile) }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB42318))) {
                            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete")
                        }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add candidate manually") } }
        item { Text("Candidate data is stored locally. Review every field before using it on a portal.", color = Color(0xFF64748B), fontSize = 11.sp) }
    }
}


@Composable
private fun ExportProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    var passphrase by remember(profile.id) { mutableStateOf("") }
    var confirmation by remember(profile.id) { mutableStateOf("") }
    var error by remember(profile.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export encrypted profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Create a passphrase of at least 12 characters. You must enter the same passphrase in the Chrome extension. It is not saved by the app.", fontSize = 12.sp)
                OutlinedTextField(value = passphrase, onValueChange = { passphrase = it; error = "" }, label = { Text("Export passphrase") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = confirmation, onValueChange = { confirmation = it; error = "" }, label = { Text("Confirm passphrase") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Text("The exported file contains personal candidate data. Keep it private and delete it after importing on the CSC computer.", color = Color(0xFF9A5A00), fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    passphrase.length < 12 -> error = "Passphrase must be at least 12 characters."
                    passphrase.length > 128 -> error = "Passphrase must be 128 characters or fewer."
                    passphrase != confirmation -> error = "Passphrases do not match."
                    else -> onExport(passphrase)
                }
            }) { Text("Choose export location") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileEditorDialog(existing: Profile?, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    var profileName by remember(existing?.id) { mutableStateOf(existing?.profileName.orEmpty()) }
    var fullName by remember(existing?.id) { mutableStateOf(existing?.fullName.orEmpty()) }
    var dob by remember(existing?.id) { mutableStateOf(existing?.dateOfBirth.orEmpty()) }
    var fatherName by remember(existing?.id) { mutableStateOf(existing?.fatherName.orEmpty()) }
    var mobile by remember(existing?.id) { mutableStateOf(existing?.mobile.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var address by remember(existing?.id) { mutableStateOf(existing?.address.orEmpty()) }
    var error by remember(existing?.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create candidate" else "Edit candidate") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(profileName, { profileName = it; error = false }, label = { Text("Candidate / profile name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dob, { dob = it }, label = { Text("Date of birth (DD/MM/YYYY)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fatherName, { fatherName = it }, label = { Text("Father's name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                if (error) Text("Please enter a candidate name.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Text("Do not save passwords, OTPs, or payment credentials.", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (profileName.isBlank()) error = true
                else onSave(Profile(existing?.id ?: UUID.randomUUID().toString(), profileName.trim(), fullName.trim(), dob.trim(), fatherName.trim(), mobile.trim(), email.trim(), address.trim()))
            }) { Text("Save securely") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FeaturePlaceholder(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
        Text("NEXT ANDROID MODULE", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(description, color = Color(0xFF64748B), fontSize = 15.sp, lineHeight = 23.sp)
    }
}
