package com.lovenote.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.glance.appwidget.updateAll
import com.lovenote.app.call.CallManager
import com.lovenote.app.call.CallOverlay
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.chat.ChatScreen
import com.lovenote.app.games.GameRepository
import com.lovenote.app.games.GamesHubScreen
import com.lovenote.app.games.ludo.LudoScreen
import com.lovenote.app.games.tictactoe.TicTacToeScreen
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
    TIC_TAC_TOE, LUDO, TRUTH_OR_DARE, WORD_GAME,
}

@Composable
internal fun Home(coupleId: String, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val chatRepository = remember(coupleId) { ChatRepository(coupleId) }
    val noteRepository = remember(coupleId) { NoteRepository(coupleId) }
    val usRepository = remember(coupleId) { UsRepository(coupleId) }
    val gameRepository = remember(coupleId) { GameRepository(coupleId) }
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
    var activeGameId by remember { mutableStateOf<String?>(null) }
    var activeGameType by remember { mutableStateOf("") }

    fun startGame(gameType: String) {
        activeGameId = null
        activeGameType = gameType
        when (gameType) {
            "tictactoe" -> navigate(HomeScreen.TIC_TAC_TOE)
            "ludo" -> navigate(HomeScreen.LUDO)
            "truthdare" -> navigate(HomeScreen.TRUTH_OR_DARE)
            "wordgame" -> navigate(HomeScreen.WORD_GAME)
        }
    }

    Box {
        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        BottomBarItem(
                            icon = Icons.Filled.Email,
                            label = "Chat",
                            selected = screen == HomeScreen.CHAT || screen == HomeScreen.SETTINGS,
                            onClick = { navigate(HomeScreen.CHAT) },
                        )
                        BottomBarItem(
                            icon = Icons.Filled.Favorite,
                            label = "Us",
                            selected = screen == HomeScreen.US || screen == HomeScreen.MEMORIES ||
                                screen == HomeScreen.TODOS,
                            onClick = { navigate(HomeScreen.US) },
                        )
                        BottomBarItem(
                            icon = Icons.Filled.Star,
                            label = "Hub",
                            selected = screen == HomeScreen.HUB || screen == HomeScreen.NOTE ||
                                screen == HomeScreen.DRAW || screen == HomeScreen.HISTORY ||
                                screen == HomeScreen.TIC_TAC_TOE || screen == HomeScreen.LUDO ||
                                screen == HomeScreen.TRUTH_OR_DARE ||
                                screen == HomeScreen.WORD_GAME,
                            onClick = { navigate(HomeScreen.HUB) },
                        )
                    }
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
                            onTicTacToe = { startGame("tictactoe") },
                            onLudo = { startGame("ludo") },
                            onTruthOrDare = { startGame("truthdare") },
                            onWordGame = { startGame("wordgame") },
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
                            gameId = activeGameId,
                            gameRepository = gameRepository,
                            myUid = chatRepository.myUid,
                            chatRepository = chatRepository,
                            onInvitePartner = {
                                val board = mapOf(
                                    "cells" to List(9) { "" },
                                    "currentPlayer" to "X",
                                )
                                val gid = gameRepository.createGame("tictactoe", myName, board)
                                chatRepository.sendGameInvite(gid, "tictactoe")
                                activeGameId = gid
                                activeGameType = "tictactoe"
                            },
                        )
                        HomeScreen.LUDO -> LudoScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                            gameId = activeGameId,
                            gameRepository = gameRepository,
                            myUid = chatRepository.myUid,
                        )
                        HomeScreen.TRUTH_OR_DARE -> TruthOrDareScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                            gameId = activeGameId,
                            gameRepository = gameRepository,
                            myUid = chatRepository.myUid,
                            chatRepository = chatRepository,
                            onInvitePartner = {
                                val board = mapOf(
                                    "currentTurn" to "p1",
                                    "spins" to 0,
                                )
                                val gid = gameRepository.createGame("truthdare", myName, board)
                                chatRepository.sendGameInvite(gid, "truthdare")
                                activeGameId = gid
                                activeGameType = "truthdare"
                            },
                        )
                        HomeScreen.WORD_GAME -> WordConnectionScreen(
                            onBack = { navigate(HomeScreen.HUB) },
                            myName = myName,
                            partnerName = partnerName,
                            gameId = activeGameId,
                            gameRepository = gameRepository,
                            myUid = chatRepository.myUid,
                            onInvitePartner = {
                                val board = mapOf(
                                    "phase" to "p1",
                                    "currentRound" to 0,
                                )
                                val gid = gameRepository.createGame("wordgame", myName, board)
                                chatRepository.sendGameInvite(gid, "wordgame")
                                activeGameId = gid
                                activeGameType = "wordgame"
                            },
                            chatRepository = chatRepository,
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
                            onGameClick = { gameId, gameType ->
                                activeGameId = gameId
                                activeGameType = gameType
                                when (gameType) {
                                    "tictactoe" -> navigate(HomeScreen.TIC_TAC_TOE)
                                    "ludo" -> navigate(HomeScreen.LUDO)
                                    "truthdare" -> navigate(HomeScreen.TRUTH_OR_DARE)
                                    "wordgame" -> navigate(HomeScreen.WORD_GAME)
                                }
                            },
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

@Composable
private fun BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 0.14f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "bgAlpha",
    )

    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
