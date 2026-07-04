package com.lovenote.app.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.glance.appwidget.updateAll
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.auth.AuthRepository
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.widget.NoteWidget
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
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = "Appearance") {
                ThemePreview()
                Spacer(Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = AppSettings.ThemeMode.entries
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = AppSettings.themeMode == mode,
                            onClick = { AppSettings.setThemeMode(context, mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        ) {
                            Text(
                                when (mode) {
                                    AppSettings.ThemeMode.SYSTEM -> "Auto"
                                    AppSettings.ThemeMode.LIGHT -> "Light"
                                    AppSettings.ThemeMode.DARK -> "Dark"
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppSettings.ACCENTS.forEach { (name, color) ->
                        AccentSwatch(
                            selected = AppSettings.accentName == name,
                            background = Brush.linearGradient(listOf(color, color)),
                            onClick = { AppSettings.setAccent(context, name) },
                        )
                    }
                    if (AppSettings.supportsDynamic()) {
                        AccentSwatch(
                            selected = AppSettings.accentName == AppSettings.DYNAMIC,
                            background = Brush.sweepGradient(
                                AppSettings.ACCENTS.values.toList() +
                                    AppSettings.ACCENTS.values.first(),
                            ),
                            onClick = { AppSettings.setAccent(context, AppSettings.DYNAMIC) },
                        )
                    }
                }
                if (AppSettings.accentName == AppSettings.DYNAMIC) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Colors follow your wallpaper (Material You)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (chatRepository != null) {
                SettingsCard(title = "Relationship") {
                    val anniversary by chatRepository.anniversaryMillis()
                        .collectAsState(initial = null)
                    val anniversaryText = anniversary?.let {
                        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Not set — tap to choose"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(vertical = 4.dp),
                    ) {
                        Text("Together since", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = anniversaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            SettingsCard(title = "Account") {
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
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = !deleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (deleting) "Deleting everything…" else "Delete account",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                deleteError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SettingsCard(title = "Notifications") {
                val powerManager = context.getSystemService(PowerManager::class.java)
                var unrestricted by remember {
                    mutableStateOf(
                        powerManager.isIgnoringBatteryOptimizations(context.packageName),
                    )
                }
                Text(
                    text = if (unrestricted) {
                        "Reliable ✓ — the system won't put OurVerse to sleep"
                    } else {
                        "Your phone may delay notifications by putting OurVerse " +
                            "to sleep in the background."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (!unrestricted) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                            unrestricted = powerManager
                                .isIgnoringBatteryOptimizations(context.packageName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Make notifications reliable")
                    }
                }
            }

            SettingsCard(title = "App update") {
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
            }

            SettingsCard(title = "About") {
                Text(
                    text = "About us",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAbout = true }
                        .padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
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
                Column {
                    Text(
                        "Version ${UpdateChecker.installedVersionName(context)}\n\n" +
                            "OurVerse is a tiny universe for two people: chat with your " +
                            "partner, leave little notes on each other's home screen, and " +
                            "keep them close as your wallpaper.\n\n" +
                            "Made with love, for the two of you.\n\n" +
                            "Developer: Arun Adhikari",
                    )
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:adhikariarun549@gmail.com"),
                                ),
                            )
                        }
                    }) {
                        Text("✉ adhikariarun549@gmail.com")
                    }
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This permanently erases everything, for both of you: all " +
                        "messages and photos, every note, your daily answers, " +
                        "special dates, and the pairing itself. Your partner will " +
                        "be unpaired.\n\nThis cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleting = true
                    deleteError = null
                    scope.launch {
                        try {
                            AuthRepository().deleteAccount(context)
                            NoteWidget().updateAll(context)
                            onLoggedOut()
                        } catch (e: Exception) {
                            deleteError = e.message ?: "Couldn't delete — try again"
                        } finally {
                            deleting = false
                        }
                    }
                }) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
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
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Two live chat bubbles so theme changes are visible without leaving the screen. */
@Composable
private fun ThemePreview() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                PreviewBubble(
                    text = "Miss you ❤",
                    container = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                PreviewBubble(
                    text = "Miss you more 🥰",
                    container = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun PreviewBubble(text: String, container: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .background(container, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccentSwatch(
    selected: Boolean,
    background: Brush,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(background, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    Color(0x33888888)
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
