package com.lovenote.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.lovenote.app.notes.NotesHistoryScreen
import com.lovenote.app.notes.SendNoteScreen
import com.lovenote.app.notify.AppVisibility
import com.lovenote.app.notify.Notifier
import com.lovenote.app.notify.NotifyState
import com.lovenote.app.pairing.PairingRepository
import com.lovenote.app.settings.AppSettings
import com.lovenote.app.settings.SettingsScreen
import com.lovenote.app.us.MemoriesScreen
import com.lovenote.app.us.UsRepository
import com.lovenote.app.us.UsScreen
import com.lovenote.app.widget.NoteWidget
import com.lovenote.app.pairing.PairingScreen
import com.lovenote.app.ui.theme.LoveNoteTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onResume() {
        super.onResume()
        AppVisibility.appVisible = true
    }

    override fun onPause() {
        AppVisibility.appVisible = false
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppSettings.load(this)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

private enum class HomeScreen { CHAT, US, MEMORIES, NOTE, HISTORY, SETTINGS }

@Composable
private fun Home(coupleId: String, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val chatRepository = remember(coupleId) { ChatRepository(coupleId) }
    val noteRepository = remember(coupleId) { NoteRepository(coupleId) }
    val usRepository = remember(coupleId) { UsRepository(coupleId) }
    var screen by remember { mutableStateOf(HomeScreen.CHAT) }
    val backStack = remember { mutableStateListOf<HomeScreen>() }

    fun navigate(to: HomeScreen) {
        if (to != screen) {
            backStack.add(screen)
            screen = to
        }
    }

    // System back retraces visited screens instead of closing the app.
    BackHandler(enabled = backStack.isNotEmpty()) {
        screen = backStack.removeAt(backStack.lastIndex)
    }

    // Presence heartbeat so the partner sees "Active now".
    LaunchedEffect(coupleId) {
        while (true) {
            runCatching { chatRepository.heartbeatPresence() }
            kotlinx.coroutines.delay(60_000)
        }
    }

    // Keep the widget's cached note fresh while the app is open, and
    // notify + vibrate on fresh notes.
    LaunchedEffect(coupleId) {
        var firstEmission = true
        noteRepository.latestFromPartner().collect { note ->
            if (note != null) {
                NoteCache.save(context, note)
                NoteWidget().updateAll(context)
                val millis = note.sentAt?.toDate()?.time ?: 0L
                if (!firstEmission && millis > NotifyState.lastNoteMillis(context)) {
                    Notifier.notifyNote(context, note.text)
                }
                NotifyState.setLastNote(context, millis)
                firstEmission = false
            }
        }
    }

    // Notify + vibrate on fresh partner messages while the app is open.
    LaunchedEffect(coupleId) {
        var firstEmission = true
        chatRepository.messages().collect { list ->
            val newest = list.firstOrNull { !it.isMine(chatRepository.myUid) }
                ?: return@collect
            val millis = newest.sentAt?.toDate()?.time ?: return@collect
            if (!firstEmission && millis > NotifyState.lastMessageMillis(context)) {
                if (AppVisibility.chatVisible) {
                    Notifier.vibrate(context)
                } else {
                    Notifier.notifyMessage(
                        context,
                        when {
                            newest.isPhoto -> "📷 Photo"
                            newest.isVoice -> "🎤 Voice note"
                            else -> newest.body
                        },
                    )
                }
            }
            NotifyState.setLastMessage(context, millis)
            firstEmission = false
        }
    }

    Scaffold(
        bottomBar = {
            // Slim bar that stays behind the keyboard when typing.
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(64.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                NavigationBarItem(
                    selected = screen == HomeScreen.CHAT || screen == HomeScreen.SETTINGS,
                    onClick = { navigate(HomeScreen.CHAT) },
                    icon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    label = { Text("Chat") },
                )
                NavigationBarItem(
                    selected = screen == HomeScreen.US || screen == HomeScreen.MEMORIES,
                    onClick = { navigate(HomeScreen.US) },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text("Us") },
                )
                NavigationBarItem(
                    selected = screen == HomeScreen.NOTE || screen == HomeScreen.HISTORY,
                    onClick = { navigate(HomeScreen.NOTE) },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    label = { Text("Notes") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            Crossfade(targetState = screen, label = "screens") { screen ->
                when (screen) {
                HomeScreen.NOTE -> SendNoteScreen(
                    repository = noteRepository,
                    onBack = { navigate(HomeScreen.CHAT) },
                    onHistoryClick = { navigate(HomeScreen.HISTORY) },
                )
                HomeScreen.HISTORY -> NotesHistoryScreen(
                    repository = noteRepository,
                    onBack = { navigate(HomeScreen.NOTE) },
                )
                HomeScreen.SETTINGS -> SettingsScreen(
                    onBack = { navigate(HomeScreen.CHAT) },
                    onLoggedOut = onLoggedOut,
                    chatRepository = chatRepository,
                )
                HomeScreen.US -> UsScreen(
                    repository = usRepository,
                    onMemoriesClick = { navigate(HomeScreen.MEMORIES) },
                )
                HomeScreen.MEMORIES -> MemoriesScreen(
                    repository = usRepository,
                    onBack = { navigate(HomeScreen.US) },
                )
                HomeScreen.CHAT -> ChatScreen(
                    repository = chatRepository,
                    onSendNoteClick = { navigate(HomeScreen.NOTE) },
                    onSettingsClick = { navigate(HomeScreen.SETTINGS) },
                )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
