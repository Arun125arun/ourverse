package com.lovenote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.glance.appwidget.updateAll
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.auth.SignInScreen
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.chat.ChatScreen
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.notes.NoteRepository
import com.lovenote.app.notes.SendNoteScreen
import com.lovenote.app.pairing.PairingRepository
import com.lovenote.app.settings.AppSettings
import com.lovenote.app.settings.SettingsScreen
import com.lovenote.app.widget.NoteWidget
import com.lovenote.app.pairing.PairingScreen
import com.lovenote.app.ui.theme.LoveNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)
        setContent {
            LoveNoteTheme {
                var signedIn by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }
                if (!signedIn) {
                    SignInScreen(onSignedIn = { signedIn = true })
                } else {
                    PairedGate(onLoggedOut = { signedIn = false })
                }
            }
        }
    }
}

/** Routes between pairing and the main app based on live couple status. */
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
            )
        else -> Home(coupleId = status?.coupleId!!, onLoggedOut = onLoggedOut)
    }
}

private enum class HomeScreen { CHAT, NOTE, SETTINGS }

@Composable
private fun Home(coupleId: String, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val chatRepository = remember(coupleId) { ChatRepository(coupleId) }
    val noteRepository = remember(coupleId) { NoteRepository(coupleId) }
    var screen by remember { mutableStateOf(HomeScreen.CHAT) }

    // Keep the widget's cached note fresh while the app is open.
    LaunchedEffect(coupleId) {
        noteRepository.latestFromPartner().collect { note ->
            if (note != null) {
                NoteCache.save(context, note)
                NoteWidget().updateAll(context)
            }
        }
    }

    when (screen) {
        HomeScreen.NOTE -> SendNoteScreen(
            repository = noteRepository,
            onBack = { screen = HomeScreen.CHAT },
        )
        HomeScreen.SETTINGS -> SettingsScreen(
            onBack = { screen = HomeScreen.CHAT },
            onLoggedOut = onLoggedOut,
        )
        HomeScreen.CHAT -> ChatScreen(
            repository = chatRepository,
            onSendNoteClick = { screen = HomeScreen.NOTE },
            onSettingsClick = { screen = HomeScreen.SETTINGS },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
