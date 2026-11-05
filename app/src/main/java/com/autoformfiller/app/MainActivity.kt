package com.autoformfiller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
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
    var selectedTab by remember { mutableStateOf(0) }
    var showAddProfile by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var profiles by remember { mutableStateOf(listOf("My Profile")) }

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
                if (selectedTab == 0) FloatingActionButton(onClick = { showAddProfile = true }, containerColor = Accent, contentColor = Color.White) { Icon(Icons.Default.Add, contentDescription = "Add profile") }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> ProfilesScreen(profiles, Modifier.padding(padding), onAdd = { showAddProfile = true })
                1 -> FeaturePlaceholder("Smart Keyboard", "Use your saved profile details directly in focused form fields. The Android keyboard service is the next development milestone.", Modifier.padding(padding))
                else -> FeaturePlaceholder("Document Scanner", "Scan an identity or education document, review the extracted text, and choose which details to save. Camera and OCR integration is coming next.", Modifier.padding(padding))
            }
        }
        if (showAddProfile) {
            AlertDialog(
                onDismissRequest = { showAddProfile = false },
                title = { Text("Create profile") },
                text = { OutlinedTextField(value = profileName, onValueChange = { profileName = it }, label = { Text("Profile name") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { if (profileName.isNotBlank()) profiles = profiles + profileName.trim(); profileName = ""; showAddProfile = false }) { Text("Create") } },
                dismissButton = { TextButton(onClick = { showAddProfile = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun ProfilesScreen(profiles: List<String>, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)) {
        item {
            Text("AUTOFILL PRO", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Your details.\nReady when you are.", color = Ink, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Save your information once and use it when filling online forms.", color = Color(0xFF64748B), fontSize = 14.sp)
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
        items(profiles.size) { index ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFEAF0FF), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Accent) } }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(profiles[index], color = Ink, fontWeight = FontWeight.SemiBold); Text("Profile details can be added in the next step", color = Color(0xFF64748B), fontSize = 12.sp) }
                    Text("LOCAL", color = Color(0xFF16835D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add another profile") } }
        item { Text("Privacy first · No cloud sync · You control what gets entered", color = Color(0xFF64748B), fontSize = 11.sp) }
    }
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
