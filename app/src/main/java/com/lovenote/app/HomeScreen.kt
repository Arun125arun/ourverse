package com.lovenote.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.lovenote.app.call.CallManager
import com.lovenote.app.call.CallOverlay
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.chat.ChatScreen
import com.lovenote.app.games.GamesHubScreen
import com.lovenote.app.games.ludo.LudoScreen
import com.lovenote.app.games.tictactoe.TicTacToeScreen
import com.lovenote.app.games.trivia.CoupleTriviaScreen
import com.lovenote.app.games.truthdare.TruthOrDareScreen
import com.lovenote.app.games.wordgame.WordConnectionScreen
import com.lovenote.app.notes.DrawNoteScreen
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.notes.NoteRepository
import com.lovenote.app.notes.NotesHistoryScreen
import com.lovenote.app.notes.SendNoteScreen
import com.lovenote.app.notify.AppVisibility
import com.lovenote.app.notify.Notifier
import com.lovenote.app.notify.NotifyState
import com.lovenote.app.settings.SettingsScreen
import com.lovenote.app.us.MemoriesScreen
import com.lovenote.app.us.TodosScreen
import com.lovenote.app.us.UsRepository
import com.lovenote.app.us.UsScreen
import com.lovenote.app.widget.NoteWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class HomeScreen {
    CHAT, US, MEMORIES, TODOS, HUB, NOTE, DRAW, HISTORY, SETTINGS,
    TIC_TAC_TOE, LUDO, TRIVIA, TRUTH_OR_DARE, WORD_GAME,
}

@Composable
internal fun Home(coupleId: String, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val chatRepository = remember(coupleId) { ChatRepository(coupleId) }
    val noteRepository = remember(coupleId) { NoteRepository(coupleId) }
    val usRepository = remember(coupleId) { UsRepository(coupleId) }
    val homeScope = rememberCoroutineScope()
    val partner by chatRepository.partnerProfile().collectAsState(initial = null)
    val myProfile by chatRepository.myProfile().collectAsState(initial = null)
    var screen by remember { mutableStateOf(HomeScreen.CHAT) }

    LaunchedEffect(coupleId) {
        CallManager.watch(context, coupleId, chatRepository.myUid)
    }
    val backStack = remember { mutableStateListOf<HomeScreen>() }

    fun navigate(to: HomeScreen) {
        if (to != screen) {
            backStack.add(screen)
            screen = to
        }
    }

    BackHandler(enabled = backStack.isNotEmpty()) {
        screen = backStack.removeAt(backStack.lastIndex)
    }

    LaunchedEffect(coupleId) {
        while (true) {
            runCatching { chatRepository.heartbeatPresence() }
            delay(60_000)
        }
    }

    LaunchedEffect(coupleId) {
        var firstEmission = true
        noteRepository.latestFromPartner().collect { note ->
            if (note != null) {
                NoteCache.save(context, note)
                NoteWidget().updateAll(context)
                val millis = note.sentAt?.toDate()?.time ?: 0L
                if (!firstEmission && millis > NotifyState.lastNoteMillis(context)) {
                    Notifier.notifyNote(
                        context,
                        note.text.ifBlank { "A doodle for you" },
                    )
                }
                NotifyState.setLastNote(context, millis)
                firstEmission = false
            }
        }
    }

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
                            newest.isPhoto -> "Photo"
                            newest.isVoice -> "Voice note"
                            else -> newest.body
                        },
                    )
                }
            }
            NotifyState.setLastMessage(context, millis)
            firstEmission = false
        }
    }

    val myName = myProfile?.name?.ifBlank { "You" } ?: "You"
    val partnerName = partner?.name?.ifBlank { "Your partner" } ?: "Your partner"

    Box {
        Scaffold(
            bottomBar = {
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
                        selected = screen == HomeScreen.US || screen == HomeScreen.MEMORIES ||
                            screen == HomeScreen.TODOS,
                        onClick = { navigate(HomeScreen.US) },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("Us") },
                    )
                    NavigationBarItem(
                        selected = screen == HomeScreen.HUB || screen == HomeScreen.NOTE ||
                            screen == HomeScreen.DRAW || screen == HomeScreen.HISTORY ||
                            screen == HomeScreen.TIC_TAC_TOE || screen == HomeScreen.LUDO ||
                            screen == HomeScreen.TRIVIA || screen == HomeScreen.TRUTH_OR_DARE ||
                            screen == HomeScreen.WORD_GAME,
                        onClick = { navigate(HomeScreen.HUB) },
                        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                        label = { Text("Hub") },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                Crossfade(targetState = screen, label = "screens") { target ->
                    when (target) {
                        HomeScreen.HUB -> GamesHubScreen(
                            onBack = { navigate(HomeScreen.CHAT) },
                            onSendNote = { navigate(HomeScreen.NOTE) },
                            onDrawNote = { navigate(HomeScreen.DRAW) },
                            onNoteHistory = { navigate(HomeScreen.HISTORY) },
                            onTicTacToe = { navigate(HomeScreen.TIC_TAC_TOE) },
                            onLudo = { navigate(HomeScreen.LUDO) },
                            onTrivia = { navigate(HomeScreen.TRIVIA) },
                            onTruthOrDare = { navigate(HomeScreen.TRUTH_OR_DARE) },
                            onWordGame = { navigate(HomeScreen.WORD_GAME) },
                            myName = myName,
                        )
                        HomeScreen.NOTE -> SendNoteScreen(
                            repository = noteRepository,
                            onBack = { navigate(HomeScreen.HUB) },
                            onHistoryClick = { navigate(HomeScreen.HISTORY) },
                            onDrawClick = { navigate(HomeScreen.DRAW) },
                        )
                        HomeScreen.DRAW -> DrawNoteScreen(
                            repository = noteRepository,
                            onBack = { navigate(HomeScreen.HUB) },
                        )
                        HomeScreen.HISTORY -> NotesHistoryScreen(
                            repository = noteRepository,
                            onBack = { navigate(HomeScreen.HUB) },
                        )
                        HomeScreen.TIC_TAC_TOE -> TicTacToeScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                            myPhotoUrl = myProfile?.photoUrl.orEmpty(),
                            partnerPhotoUrl = partner?.photoUrl.orEmpty(),
                        )
                        HomeScreen.LUDO -> LudoScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                        )
                        HomeScreen.TRIVIA -> CoupleTriviaScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                        )
                        HomeScreen.TRUTH_OR_DARE -> TruthOrDareScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                        )
                        HomeScreen.WORD_GAME -> WordConnectionScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                        )
                        HomeScreen.SETTINGS -> SettingsScreen(
                            onBack = { navigate(HomeScreen.CHAT) },
                            onLoggedOut = onLoggedOut,
                            chatRepository = chatRepository,
                        )
                        HomeScreen.US -> UsScreen(
                            repository = usRepository,
                            onMemoriesClick = { navigate(HomeScreen.MEMORIES) },
                            onTodosClick = { navigate(HomeScreen.TODOS) },
                        )
                        HomeScreen.MEMORIES -> MemoriesScreen(
                            repository = usRepository,
                            onBack = { navigate(HomeScreen.US) },
                        )
                        HomeScreen.TODOS -> TodosScreen(
                            repository = usRepository,
                            onBack = { navigate(HomeScreen.US) },
                            onRemind = { title ->
                                homeScope.launch {
                                    runCatching { chatRepository.send("Reminder: $title") }
                                }
                            },
                        )
                        HomeScreen.CHAT -> ChatScreen(
                            repository = chatRepository,
                            onSettingsClick = { navigate(HomeScreen.SETTINGS) },
                        )
                    }
                }
            }
        }
        CallOverlay(
            partnerName = partner?.name ?: "Your partner",
            partnerPhoto = partner?.photoUrl ?: "",
        )
    }
}
