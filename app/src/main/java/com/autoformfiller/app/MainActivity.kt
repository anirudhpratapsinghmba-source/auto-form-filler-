package com.autoformfiller.app

import android.os.Bundle
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
import androidx.room.Room
import com.autoformfiller.app.data.AppDatabase
import com.autoformfiller.app.data.Profile
import com.autoformfiller.app.data.ProfileRepository
import kotlinx.coroutines.launch
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
    val repository = remember {
        ProfileRepository(AppDatabase.getInstance(context).profileDao())
    }
    val scope = rememberCoroutineScope()
    val profiles by repository.observeProfiles().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Canvas)) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profiles") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Keyboard, null) }, label = { Text("Keyboard") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Description, null) }, label = { Text("Scanner") })
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
                    onDelete = { profile -> scope.launch { repository.delete(profile.id) } }
                )
                1 -> FeaturePlaceholder("Smart Keyboard", "The profile database is ready. Next we will add the Android keyboard service so you can insert selected details directly into supported form fields.", Modifier.padding(padding))
                else -> FeaturePlaceholder("Document Scanner", "Camera capture and on-device OCR are the next milestone. Extracted details will be reviewed by you before they are saved to an encrypted profile.", Modifier.padding(padding))
            }
        }
        if (showEditor) {
            ProfileEditorDialog(
                existing = editingProfile,
                onDismiss = { showEditor = false },
                onSave = { profile ->
                    scope.launch { repository.save(profile) }
                    showEditor = false
                }
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
    onDelete: (Profile) -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)
    ) {
        item {
            Text("AUTOFILL PRO", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Your details.\nReady when you are.", color = Ink, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Save your information once. Your profile fields are encrypted on this device.", color = Color(0xFF64748B), fontSize = 14.sp)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Ink), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("YOUR WORKSPACE", color = Color(0xFFB9CCF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("${profiles.size}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("Saved profiles", color = Color(0xFFD5DEEF), fontSize = 13.sp)
                }
            }
        }
        item { Text("PROFILES", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
        if (profiles.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, null, tint = Accent, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No profiles yet", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Add your first profile to get started.", color = Color(0xFF64748B), fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onAdd) { Text("Create profile") }
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
                            Text(profile.fullName.ifBlank { "Personal profile" }, color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Text("ENCRYPTED", color = Color(0xFF16835D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    val summary = listOf(profile.mobile, profile.email, profile.address).filter { it.isNotBlank() }
                    if (summary.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(summary.joinToString("  ·  "), color = Color(0xFF64748B), fontSize = 12.sp, maxLines = 2)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEdit(profile) }) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit") }
                        TextButton(onClick = { onDelete(profile) }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB42318))) {
                            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete")
                        }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add another profile") } }
        item { Text("Private by design · Stored locally · No cloud sync", color = Color(0xFF64748B), fontSize = 11.sp) }
    }
}

@Composable
private fun ProfileEditorDialog(
    existing: Profile?,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit
) {
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
        title = { Text(if (existing == null) "Create profile" else "Edit profile") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(profileName, { profileName = it; error = false }, label = { Text("Profile name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dob, { dob = it }, label = { Text("Date of birth (DD/MM/YYYY)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fatherName, { fatherName = it }, label = { Text("Father's name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                if (error) Text("Please enter a profile name.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Text("Avoid saving passwords, OTPs, or payment details.", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (profileName.isBlank()) error = true
                else onSave(Profile(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    profileName = profileName.trim(),
                    fullName = fullName.trim(),
                    dateOfBirth = dob.trim(),
                    fatherName = fatherName.trim(),
                    mobile = mobile.trim(),
                    email = email.trim(),
                    address = address.trim()
                ))
            }) { Text("Save securely") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FeaturePlaceholder(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
        Text("COMING NEXT", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(description, color = Color(0xFF64748B), fontSize = 15.sp, lineHeight = 23.sp)
    }
}
