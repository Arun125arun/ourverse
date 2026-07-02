package com.lovenote.app.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.chat.ChatRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    chatRepository: ChatRepository? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSettings.ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = AppSettings.themeMode == mode,
                        onClick = { AppSettings.setThemeMode(context, mode) },
                        label = {
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Color", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSettings.ACCENTS.forEach { (name, color) ->
                    Column(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (AppSettings.accentName == name) 3.dp else 1.dp,
                                color = if (AppSettings.accentName == name) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    Color(0x33000000)
                                },
                                shape = CircleShape,
                            )
                            .clickable { AppSettings.setAccent(context, name) },
                    ) {}
                }
            }

            if (chatRepository != null) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                SectionTitle("Relationship")
                val anniversary by chatRepository.anniversaryMillis()
                    .collectAsState(initial = null)
                val anniversaryText = anniversary?.let {
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Not set — tap to choose"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp),
                ) {
                    Text("Together since", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = anniversaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SectionTitle("Account")
            val user = FirebaseAuth.getInstance().currentUser
            Text(
                text = user?.displayName ?: "",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log out")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SectionTitle("App update")
            Text(
                text = "Installed version: ${UpdateChecker.installedVersionName(context)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        checkingUpdate = true
                        updateStatus = null
                        val latest = UpdateChecker.fetchLatest()
                        checkingUpdate = false
                        when {
                            latest == null ->
                                updateStatus = "Couldn't check — are you online?"
                            latest.versionCode > UpdateChecker.installedVersionCode(context) ->
                                availableUpdate = latest
                            else ->
                                updateStatus = "You're up to date ✓"
                        }
                    }
                },
                enabled = !checkingUpdate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (checkingUpdate) "Checking…" else "Check for updates")
            }
            updateStatus?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SectionTitle("About")
            Text(
                text = "About us",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAbout = true }
                    .padding(vertical = 12.dp),
            )
        }
    }

    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = { Text("Update available ❤") },
            text = {
                Text(
                    "Version ${update.versionName} is ready. It will download in " +
                        "your browser — open the file when it finishes to install.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    availableUpdate = null
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(update.url)),
                        )
                    }
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { availableUpdate = null }) { Text("Later") }
            },
        )
    }

    if (showDatePicker && chatRepository != null) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        scope.launch { runCatching { chatRepository.setAnniversary(millis) } }
                    }
                    showDatePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Close") }
            },
            title = { Text("OurVerse ❤") },
            text = {
                Text(
                    "Version ${UpdateChecker.installedVersionName(context)}\n\n" +
                        "OurVerse is a tiny universe for two people: chat with your " +
                        "partner, leave little notes on each other's home screen, and " +
                        "keep them close as your wallpaper.\n\n" +
                        "Made with love, for the two of you.",
                )
            },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to see your messages.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch {
                        runCatching {
                            CredentialManager.create(context)
                                .clearCredentialState(ClearCredentialStateRequest())
                        }
                        FirebaseAuth.getInstance().signOut()
                        onLoggedOut()
                    }
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))
}
