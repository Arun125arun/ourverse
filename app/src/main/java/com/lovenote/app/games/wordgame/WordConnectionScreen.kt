package com.lovenote.app.games.wordgame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.games.GameRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val PROMPTS = listOf(
    "A color", "An animal", "A food", "A city", "A movie",
    "A song", "A holiday destination", "A superpower", "A dessert", "A sport",
    "A flower", "A cartoon character", "A drink", "A season", "A musical instrument",
    "A hobby", "A book", "A TV show", "A vehicle", "A celebrity",
    "An emoji", "A feeling", "A place in our city", "A type of dance", "A breakfast item",
    "Something soft", "Something expensive", "Something funny", "A childhood toy", "A dream job",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordConnectionScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    gameId: String? = null,
    gameRepository: GameRepository? = null,
    myUid: String = "",
    onInvitePartner: suspend () -> Unit,
    chatRepository: ChatRepository? = null,
) {
    val scope = rememberCoroutineScope()

    val isOnline = gameRepository != null && gameId != null
    val firestoreSession by remember(gameId, gameRepository) {
        if (gameRepository != null && gameId != null) gameRepository.observeGame(gameId)
        else flowOf(null)
    }.collectAsState(initial = null)
    val session = firestoreSession

    var mode by remember { mutableStateOf<String?>(null) }
    var showModeDialog by remember { mutableStateOf(gameId == null) }
    var isSendingInvite by remember { mutableStateOf(false) }

    val selectedPrompts = remember { PROMPTS.shuffled().take(8) }
    var phase by remember { mutableStateOf("intro") }
    var currentRound by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val p1Answers = remember { mutableStateListOf<String>() }
    val p2Answers = remember { mutableStateListOf<String>() }
    var waitingForPartner by remember { mutableStateOf(isOnline && session?.p2Uid.isNullOrEmpty()) }
    var gameEndSent by remember { mutableStateOf(false) }

    // --- Local mode variables ---
    var localTurn by remember { mutableIntStateOf(1) }

    // --- Online mode player names ---
    val myPlayerName = if (isOnline && session != null) {
        if (session.p1Uid == myUid) session.p1Name else session.p2Name
    } else myName
    val partnerPlayerName = if (isOnline && session != null) {
        if (session.p1Uid == myUid) session.p2Name else session.p1Name
    } else partnerName

    val myPhase = if (isOnline && session != null) {
        if (session.p1Uid == myUid) "p1" else "p2"
    } else "p1"

    // Auto-join as p2 when joining from a chat invite
    LaunchedEffect(session, isOnline) {
        if (!isOnline || session == null) return@LaunchedEffect
        if (session.p2Uid.isEmpty() && session.p1Uid != myUid && gameRepository != null) {
            gameRepository.joinGame(gameId!!, myName)
        }
        waitingForPartner = session.p2Uid.isNullOrEmpty()
    }

    // Set mode when gameId arrives from online invite creation
    LaunchedEffect(gameId) {
        if (gameId != null && mode == null) {
            mode = "online"
            showModeDialog = false
        }
    }

    // Sync state from Firestore in online mode
    LaunchedEffect(session) {
        if (session == null || !isOnline) return@LaunchedEffect
        currentRound = (session.board["currentRound"] as? Number)?.toInt() ?: 0
        val remotePhase = session.board["phase"] as? String
        if (remotePhase == "results" && phase != "results") {
            phase = "results"
            val rp1 = session.board["p1Answers"] as? List<*>
            val rp2 = session.board["p2Answers"] as? List<*>
            if (rp1 != null) { p1Answers.clear(); p1Answers.addAll(rp1.map { it?.toString() ?: "" }) }
            if (rp2 != null) { p2Answers.clear(); p2Answers.addAll(rp2.map { it?.toString() ?: "" }) }
        }
        if (remotePhase == "p2" && phase != "p2" && myPhase == "p2") {
            phase = "p2"
            currentRound = 0
            val rp1 = session.board["p1Answers"] as? List<*>
            if (rp1 != null) { p1Answers.clear(); p1Answers.addAll(rp1.map { it?.toString() ?: "" }) }
        }
        if (session.winner == "ended" && !gameEndSent && chatRepository != null) {
            gameEndSent = true
            scope.launch {
                runCatching { chatRepository.sendGameEnd(gameId!!, "wordgame", "Thanks for playing! \uD83D\uDD24") }
            }
        }
    }

    // --- Mode choice dialog ---
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { /* cannot dismiss */ },
            icon = { Text("\uD83D\uDD24", fontSize = 40.sp) },
            title = {
                Text(
                    "Word Connection",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Text(
                    if (isSendingInvite) "Creating game..."
                    else "How well do you think alike? Choose how to play:",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSendingInvite = true
                        scope.launch {
                            runCatching { onInvitePartner() }
                            isSendingInvite = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSendingInvite,
                ) {
                    if (isSendingInvite) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Send Invitation")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mode = "local"
                        showModeDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSendingInvite,
                ) {
                    Text("Play Locally")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Word Connection")
                        val subtitle = when {
                            mode == null -> ""
                            mode == "online" && waitingForPartner -> "Waiting for partner..."
                            mode == "online" -> "$myPlayerName's turn"
                            mode == "local" && localTurn == 1 -> "Player 1's turn"
                            mode == "local" && localTurn == 2 -> "Player 2's turn"
                            else -> ""
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phase",
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) { currentPhase ->
            when (currentPhase) {
                "intro" -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "\uD83D\uDD24", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Word Connection",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "How well do you think alike? Both write the first word that comes to mind!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    if (mode == "online" && waitingForPartner) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Waiting for your partner to join...",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else if (mode == "local" && localTurn == 2 && p1Answers.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Button(onClick = {
                            if (mode == "online") {
                                phase = myPhase
                            } else if (localTurn == 2) {
                                phase = "p2"
                            } else {
                                phase = "p1"
                            }
                            currentRound = 0
                        }) {
                            Text(
                                if (mode == "local" && localTurn == 2) "Player 2: Start"
                                else "Start Playing",
                            )
                        }
                    }
                }

                "p1", "p2" -> {
                    val isMyTurnOnline = currentPhase == myPhase
                    val isMyTurnLocal = mode == "local" && (
                        (localTurn == 1 && currentPhase == "p1") ||
                            (localTurn == 2 && currentPhase == "p2")
                        )
                    val canType = if (mode == "online") isMyTurnOnline else isMyTurnLocal
                    val playerName = if (mode == "online") {
                        if (currentPhase == "p1") myPlayerName else partnerPlayerName
                    } else {
                        if (currentPhase == "p1" || (mode == "local" && localTurn == 1)) "Player 1"
                        else "Player 2"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "$playerName's turn",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${currentRound + 1} of ${selectedPrompts.size}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(Modifier.height(20.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = selectedPrompts[currentRound],
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(24.dp),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        TextField(
                            value = input,
                            onValueChange = { input = it.take(30) },
                            placeholder = { Text("Type your word...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = canType,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val word = input.trim()
                                if (currentPhase == "p1") {
                                    p1Answers.add(word)
                                } else {
                                    p2Answers.add(word)
                                }
                                input = ""

                                if (currentRound < selectedPrompts.lastIndex) {
                                    currentRound++
                                    // Online: sync round progress
                                    if (mode == "online" && gameRepository != null && gameId != null) {
                                        scope.launch {
                                            runCatching {
                                                gameRepository.makeMove(
                                                    gameId,
                                                    mapOf(
                                                        "phase" to currentPhase,
                                                        "currentRound" to currentRound,
                                                        "p1Answers" to p1Answers.toList(),
                                                        "p2Answers" to p2Answers.toList(),
                                                    ),
                                                    "",
                                                    mapOf("round" to currentRound, "by" to myUid),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Finished all prompts for this phase
                                    if (currentPhase == "p1") {
                                        if (mode == "online") {
                                            phase = "results"
                                            // Sync p2 answers placeholder so p2 can see p1 results
                                            scope.launch {
                                                runCatching {
                                                    gameRepository?.makeMove(
                                                        gameId!!,
                                                        mapOf(
                                                            "phase" to "p2",
                                                            "currentRound" to 0,
                                                            "p1Answers" to p1Answers.toList(),
                                                        ),
                                                        "",
                                                        mapOf("action" to "p1_done", "by" to myUid),
                                                    )
                                                }
                                            }
                                        } else {
                                            // Local: transition to p2
                                            phase = "intro"
                                            localTurn = 2
                                            currentRound = 0
                                        }
                                    } else {
                                        phase = "results"
                                        if (mode == "online" && gameRepository != null && gameId != null) {
                                            scope.launch {
                                                runCatching {
                                                    gameRepository.makeMove(
                                                        gameId,
                                                        mapOf(
                                                            "phase" to "results",
                                                            "p2Answers" to p2Answers.toList(),
                                                        ),
                                                        "",
                                                        mapOf("action" to "results", "by" to myUid),
                                                        winner = "ended",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = input.isNotBlank() && canType,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Submit") }
                    }
                }

                "results" -> {
                    val matches = selectedPrompts.indices.count {
                        it < p1Answers.size && it < p2Answers.size &&
                            p1Answers[it].equals(p2Answers[it], ignoreCase = true) &&
                            p1Answers[it].isNotBlank()
                    }
                    val displayP1 = if (mode == "online") myPlayerName else "Player 1"
                    val displayP2 = if (mode == "online") partnerPlayerName else "Player 2"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Results \uD83D\uDD24",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(12.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "$matches/${selectedPrompts.size}",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = when {
                                        matches >= 6 -> "Incredible connection! \uD83D\uDC95"
                                        matches >= 4 -> "Pretty in sync! \uD83D\uDC95"
                                        else -> "Keep learning about each other! \uD83D\uDC95"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        for (i in selectedPrompts.indices) {
                            val w1 = p1Answers.getOrNull(i) ?: ""
                            val w2 = p2Answers.getOrNull(i) ?: ""
                            val match = w1.equals(w2, ignoreCase = true) && w1.isNotBlank()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (match) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        selectedPrompts[i],
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            "$displayP1: $w1",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            "$displayP2: $w2",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                p1Answers.clear()
                                p2Answers.clear()
                                currentRound = 0
                                input = ""
                                gameEndSent = false
                                localTurn = 1
                                phase = "intro"
                            }) { Text("Play Again") }
                            OutlinedButton(onClick = onBack) { Text("Back") }
                        }
                    }
                }
            }
        }
    }
}
