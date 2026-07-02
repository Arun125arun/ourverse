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
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.auth.SignInScreen
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.chat.ChatScreen
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.notes.NoteRepository
import com.lovenote.app.notes.SendNoteScreen
import com.lovenote.app.pairing.PairingRepository
import com.lovenote.app.pairing.PairingScreen
import com.lovenote.app.ui.theme.LoveNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoveNoteTheme {
                var signedIn by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }
                if (!signedIn) {
                    SignInScreen(onSignedIn = { signedIn = true })
                } else {
                    PairedGate()
                }
            }
        }
    }
}

/** Routes between pairing and the main app based on live couple status. */
@Composable
private fun PairedGate() {
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
        else -> Home(coupleId = status?.coupleId!!)
    }
}

@Composable
private fun Home(coupleId: String) {
    val context = LocalContext.current
    val chatRepository = remember(coupleId) { ChatRepository(coupleId) }
    val noteRepository = remember(coupleId) { NoteRepository(coupleId) }
    var composingNote by remember { mutableStateOf(false) }

    // Keep the widget's cached note fresh while the app is open.
    LaunchedEffect(coupleId) {
        noteRepository.latestFromPartner().collect { note ->
            if (note != null) NoteCache.save(context, note)
        }
    }

    if (composingNote) {
        SendNoteScreen(
            repository = noteRepository,
            onBack = { composingNote = false },
        )
    } else {
        ChatScreen(
            repository = chatRepository,
            onSendNoteClick = { composingNote = true },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
