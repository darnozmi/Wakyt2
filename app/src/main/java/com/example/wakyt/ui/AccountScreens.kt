package com.example.wakyt.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wakyt.data.AppRepository
import com.example.wakyt.data.TaskState
import com.example.wakyt.data.UserProfile
import com.example.wakyt.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onLogout: () -> Unit,
) {
    val profile by AppRepository.userProfile.collectAsState(initial = UserProfile(name = "John Doe", email = "john@example.com", photoUri = null))
    val tasks by AppRepository.tasks.collectAsState(initial = emptyList())

    val completed = tasks.count { it.status == TaskState.DONE }
    val pending = tasks.count { it.status != TaskState.DONE }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile information
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAvatar(uri = profile.photoUri)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenEditProfile) { Icon(Icons.Default.Edit, contentDescription = "Edit Profile") }
                }
            }

            // Task statistics
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(title = "Tasks Completed", value = completed, modifier = Modifier.weight(1f))
                StatCard(title = "Tasks Pending", value = pending, modifier = Modifier.weight(1f))
            }

            // Preferences
            SectionHeader("Preferences")
            SettingsItem(title = "Notifications", onClick = onOpenNotifications)
            SettingsItem(title = "App Settings", onClick = onOpenAppSettings)

            // Support & Information
            SectionHeader("Support & Information")
            SettingsItem(title = "Privacy Policy", onClick = onOpenPrivacy)
            SettingsItem(title = "FAQs", onClick = onOpenFaq)

            // Account Management
            SectionHeader("Account Management")
            SettingsItem(title = "Change Password", onClick = onOpenChangePassword)
            var showLogout by remember { mutableStateOf(false) }
            SettingsItem(title = "Log Out", isDestructive = true, onClick = { showLogout = true })
            if (showLogout) {
                AlertDialog(
                    onDismissRequest = { showLogout = false },
                    confirmButton = {
                        TextButton(onClick = { showLogout = false; onLogout() }) { Text("Log Out", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancel") } },
                    title = { Text("Are you sure you want to log out?") }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingsItem(title: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = color)
    }
    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun StatCard(title: String, value: Int, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileAvatar(uri: Uri?) {
    if (uri != null) {
        AsyncImage(
            model = uri,
            contentDescription = "Profile picture",
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray)
        )
    } else {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("👤")
        }
    }
}

// ---- Sub‑screens ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val profile by AppRepository.userProfile.collectAsState(initial = UserProfile(name = "John Doe", email = "john@example.com", photoUri = null))
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var photoUri by remember { mutableStateOf<Uri?>(profile.photoUri) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> photoUri = uri }

    Scaffold(containerColor = Color.White, topBar = {
        CenterAlignedTopAppBar(title = { Text("Edit Profile") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileAvatar(uri = photoUri)
                OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Change photo") }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Cancel") }
                Button(onClick = { AppRepository.updateProfile(name, email, photoUri); onBack() }, enabled = name.isNotBlank() && email.isNotBlank()) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    val settings by AppRepository.settings.collectAsState(initial = AppSettings())
    var enabled by remember { mutableStateOf(settings.notificationsEnabled) }
    var minutes by remember { mutableStateOf(settings.reminderMinutesBefore) }

    Scaffold(containerColor = Color.White, topBar = {
        CenterAlignedTopAppBar(title = { Text("Notifications") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enable notifications", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            OutlinedTextField(value = minutes.toString(), onValueChange = { minutes = it.toIntOrNull() ?: minutes }, label = { Text("Reminder minutes before") })
            Button(onClick = { AppRepository.updateSettings(notificationsEnabled = enabled, reminderMinutesBefore = minutes); onBack() }) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val settings by AppRepository.settings.collectAsState(initial = AppSettings())
    var theme by remember { mutableStateOf(settings.theme) }
    var language by remember { mutableStateOf(settings.language) }

    Scaffold(containerColor = Color.White, topBar = {
        CenterAlignedTopAppBar(title = { Text("App Settings") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = theme, onValueChange = { theme = it }, label = { Text("Theme (light/dark)") })
            OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") })
            Button(onClick = { AppRepository.updateSettings(theme = theme, language = language); onBack() }) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(containerColor = Color.White, topBar = {
        CenterAlignedTopAppBar(title = { Text("Privacy Policy") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Surface(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding).padding(16.dp)) {
            Text("This is a placeholder for the Privacy Policy. Replace with actual content or WebView.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqsScreen(onBack: () -> Unit) {
    var expanded by remember { mutableStateOf<Int?>(null) }
    val qs = listOf(
        "How to add a task?" to "Open the Add tab and fill the task form.",
        "How to create a project?" to "In Add > Project, select a group and set dates.",
        "How to mark task done?" to "In Home, expand the project and tap the check icon."
    )
    Scaffold(containerColor = Color.White, topBar = {
        CenterAlignedTopAppBar(title = { Text("FAQs") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            qs.forEachIndexed { index, pair ->
                val isOpen = expanded == index
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { expanded = if (isOpen) null else index }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(pair.first, fontWeight = FontWeight.SemiBold)
                        if (isOpen) {
                            Spacer(Modifier.height(6.dp))
                            Text(pair.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(containerColor = Color.White, topBar = {
        CenterAlignedTopAppBar(title = { Text("Change Password") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Current password") })
            OutlinedTextField(value = new, onValueChange = { new = it }, label = { Text("New password") })
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm new password") })
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                error = when {
                    new.length < 6 -> "Password must be at least 6 characters"
                    new != confirm -> "Passwords do not match"
                    current.isBlank() -> "Enter current password"
                    else -> null
                }
                if (error == null) onBack()
            }) { Text("Save") }
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true)
            Button(onClick = onLogin, enabled = email.isNotBlank() && password.isNotBlank()) { Text("Login") }
        }
    }
}
