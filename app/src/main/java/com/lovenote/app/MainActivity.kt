package com.lovenote.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.R
import com.lovenote.app.auth.SignInScreen
import com.lovenote.app.auth.WelcomeScreen
import com.lovenote.app.pairing.PairingRepository
import com.lovenote.app.pairing.PairingScreen
import com.lovenote.app.settings.Changelog
import com.lovenote.app.settings.UpdateChecker
import com.lovenote.app.ui.theme.LoveNoteTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onResume() {
        super.onResume()
        com.lovenote.app.notify.AppVisibility.appVisible = true
    }

    override fun onPause() {
        com.lovenote.app.notify.AppVisibility.appVisible = false
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.lovenote.app.settings.AppSettings.load(this)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        com.lovenote.app.notify.ListenerService.start(this)

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentVersion = UpdateChecker.installedVersionCode(this)
        val lastSeenVersion = prefs.getLong("lastSeenVersion", -1L)
        val updatedFromOlder = lastSeenVersion in 1 until currentVersion
        prefs.edit().putLong("lastSeenVersion", currentVersion).apply()

        setContent {
            LoveNoteTheme {
                val context = LocalContext.current

                var showWhatsNew by remember { mutableStateOf(updatedFromOlder) }
                if (showWhatsNew) {
                    AlertDialog(
                        onDismissRequest = { showWhatsNew = false },
                        confirmButton = {
                            TextButton(onClick = { showWhatsNew = false }) { Text(stringResource(R.string.whats_new_confirm)) }
                        },
                        title = {
                            Text(stringResource(R.string.whats_new_title, UpdateChecker.installedVersionName(this)))
                        },
                        text = {
                            Column {
                                Changelog.notesFor(currentVersion).forEach { note ->
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                            }
                        },
                    )
                }

                var availableUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

                val onboardingPrefs = remember { getSharedPreferences("onboarding", MODE_PRIVATE) }
                var hasSeenWelcome by remember {
                    mutableStateOf(onboardingPrefs.getBoolean("seenWelcome", false))
                }
                var signedIn by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }

                LaunchedEffect(Unit) {
                    if (!showWhatsNew && signedIn && hasSeenWelcome) {
                        val latest = UpdateChecker.fetchLatest()
                        if (latest != null && latest.versionCode > currentVersion) {
                            availableUpdate = latest
                        }
                    }
                }

                availableUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { availableUpdate = null },
                        title = { Text(stringResource(R.string.update_available_title)) },
                        text = {
                            Text(stringResource(R.string.update_available_message, update.versionName))
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                availableUpdate = null
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(update.url)),
                                    )
                                }
                            }) { Text(stringResource(R.string.download_button)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { availableUpdate = null }) {
                                Text(stringResource(R.string.later_button))
                            }
                        },
                    )
                }

                when {
                    !hasSeenWelcome -> WelcomeScreen(onGetStarted = {
                        onboardingPrefs.edit().putBoolean("seenWelcome", true).apply()
                        hasSeenWelcome = true
                    })
                    !signedIn -> SignInScreen(onSignedIn = { signedIn = true })
                    else -> PairedGate(onLoggedOut = { signedIn = false })
                }
            }
        }
    }
}

@Composable
private fun PairedGate(onLoggedOut: () -> Unit) {
    val repository = remember { PairingRepository() }
    val status by remember { repository.observeStatus() }
        .collectAsState(initial = null)

    when {
        status == null -> LoadingScreen()
        status?.coupleId == null || status?.partnerJoined == false ->
            PairingScreen(
                waitingCode = status?.takeIf { it.coupleId != null }?.inviteCode,
                repository = repository,
                onLoggedOut = onLoggedOut,
            )
        else -> {
            val coupleId = status?.coupleId
            if (coupleId != null) Home(coupleId = coupleId, onLoggedOut = onLoggedOut)
            else LoadingScreen()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
