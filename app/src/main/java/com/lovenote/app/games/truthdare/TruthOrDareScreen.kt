package com.lovenote.app.games.truthdare

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.random.Random

private data class TruthDareItem(val type: String, val text: String, val difficulty: String)

private val TRUTHS = listOf(
    TruthDareItem("Truth", "What's your most embarrassing moment with me?", "Medium"),
    TruthDareItem("Truth", "What's a secret you've never told me?", "Hard"),
    TruthDareItem("Truth", "When did you first know you loved me?", "Easy"),
    TruthDareItem("Truth", "What's your favorite memory of us?", "Easy"),
    TruthDareItem("Truth", "Have you ever checked my phone?", "Medium"),
    TruthDareItem("Truth", "What's something I do that annoys you?", "Medium"),
    TruthDareItem("Truth", "What's your biggest insecurity about us?", "Hard"),
    TruthDareItem("Truth", "If you could change one thing about me, what?", "Hard"),
    TruthDareItem("Truth", "What's the sweetest thing I've done for you?", "Easy"),
    TruthDareItem("Truth", "Have you ever pretended to like a gift?", "Medium"),
    TruthDareItem("Truth", "What's a fantasy you've never told me?", "Hard"),
    TruthDareItem("Truth", "When do you feel most loved by me?", "Easy"),
    TruthDareItem("Truth", "What song reminds you of me?", "Easy"),
    TruthDareItem("Truth", "Have you ever stalked my ex on social media?", "Medium"),
    TruthDareItem("Truth", "What's your dream proposal scenario?", "Easy"),
    TruthDareItem("Truth", "What's a dealbreaker you haven't told me about?", "Hard"),
    TruthDareItem("Truth", "Do you believe in soulmates?", "Easy"),
    TruthDareItem("Truth", "What's something you're glad I don't know?", "Hard"),
)

private val DARES = listOf(
    TruthDareItem("Dare", "Give me a 60-second hug", "Easy"),
    TruthDareItem("Dare", "Serenade me with any song", "Medium"),
    TruthDareItem("Dare", "Do 10 push-ups right now", "Medium"),
    TruthDareItem("Dare", "Send me a voice note saying why you love me", "Easy"),
    TruthDareItem("Dare", "Speak in an accent for the next 5 minutes", "Easy"),
    TruthDareItem("Dare", "Let me post a photo of you on my story", "Medium"),
    TruthDareItem("Dare", "Dance with me for one full song", "Easy"),
    TruthDareItem("Dare", "Write me a love poem in 2 minutes", "Medium"),
    TruthDareItem("Dare", "Give me a foot massage for 3 minutes", "Easy"),
    TruthDareItem("Dare", "Act like our first date for the next 5 minutes", "Medium"),
    TruthDareItem("Dare", "Screenshot your search history and show me", "Hard"),
    TruthDareItem("Dare", "Let me style your hair however I want", "Medium"),
    TruthDareItem("Dare", "Make me breakfast in bed tomorrow", "Easy"),
    TruthDareItem("Dare", "Record yourself saying 10 things you love about me", "Medium"),
    TruthDareItem("Dare", "Hold my hand in public for the next hour", "Easy"),
    TruthDareItem("Dare", "Imitate me for 1 minute", "Medium"),
    TruthDareItem("Dare", "Go a full hour without your phone", "Hard"),
    TruthDareItem("Dare", "Draw a portrait of me in 3 minutes", "Medium"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruthOrDareScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    gameId: String? = null,
    gameRepository: GameRepository? = null,
    myUid: String = "",
    onInvitePartner: (suspend () -> Unit)? = null,
    chatRepository: ChatRepository? = null,
) {
    val scope = rememberCoroutineScope()

    // --- Mode decision ---
    var showModeDialog by remember { mutableStateOf(gameId == null) }
    var localMode by remember { mutableStateOf(gameId == null) }
    var onlineMode by remember { mutableStateOf(gameId != null) }

    // --- Firestore session (online mode) ---
    val gid = gameId
    val repo = gameRepository
    val firestoreSession by remember(gid, repo) {
        if (repo != null && gid != null) repo.observeGame(gid)
        else flowOf(null)
    }.collectAsState(initial = null)

    val session = firestoreSession

    // --- Shared UI state ---
    var currentItem by remember { mutableStateOf<TruthDareItem?>(null) }
    var history by remember { mutableStateOf(listOf<Pair<String, TruthDareItem>>()) }
    var spins by remember { mutableIntStateOf(0) }

    // --- Local mode state ---
    var localTurn by remember { mutableIntStateOf(1) } // 1 or 2
    var gameEndSent by remember { mutableStateOf(false) }

    // --- Online mode state ---
    var waitingForPartner by remember { mutableStateOf(onlineMode && session?.p2Uid.isNullOrEmpty()) }

    // Computed player names
    val myPlayerName = if (onlineMode && session != null) {
        if (session.p1Uid == myUid) session.p1Name else session.p2Name
    } else myName

    val partnerPlayerName = if (onlineMode && session != null) {
        if (session.p1Uid == myUid) session.p2Name else session.p1Name
    } else partnerName

    val isMyTurn = if (onlineMode && session != null) {
        session.currentTurn == myUid
    } else if (localMode) {
        localTurn == 1
    } else true

    val currentTurnName = if (onlineMode && session != null) {
        if (isMyTurn) myPlayerName else partnerPlayerName
    } else if (localMode) {
        if (localTurn == 1) myName else partnerName
    } else myName

    // --- ModeChoiceDialog ---
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = {
                Text("Truth or Dare", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("How would you like to play?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showModeDialog = false
                    onlineMode = true
                    localMode = false
                    scope.launch {
                        runCatching { onInvitePartner?.invoke() }
                    }
                }) {
                    Text("Send Invitation")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showModeDialog = false
                    localMode = true
                    onlineMode = false
                }) {
                    Text("Play Locally")
                }
            },
        )
    }

    // --- Auto-join as player 2 (online, joined from invite) ---
    LaunchedEffect(session, onlineMode) {
        if (!onlineMode || session == null) return@LaunchedEffect
        if (session.p2Uid.isEmpty() && session.p1Uid != myUid && repo != null) {
            repo.joinGame(gid!!, myName)
        }
        waitingForPartner = session.p2Uid.isNullOrEmpty()
    }

    // --- Sync state from Firestore (online) ---
    LaunchedEffect(session) {
        if (session == null || !onlineMode) return@LaunchedEffect
        spins = (session.board["spins"] as? Number)?.toInt() ?: 0
        val histRaw = session.board["history"] as? List<*>
        if (histRaw != null) {
            history = histRaw.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val name = map["name"] as? String ?: ""
                val type = map["type"] as? String ?: "Truth"
                val text = map["text"] as? String ?: ""
                val diff = map["difficulty"] as? String ?: "Medium"
                name to TruthDareItem(type, text, diff)
            }
        }
        val lastType = session.board["lastChoice"] as? String
        val lastText = session.board["lastText"] as? String
        val lastDiff = session.board["lastDiff"] as? String
        if (lastType != null && lastText != null && currentItem == null) {
            currentItem = TruthDareItem(lastType, lastText, lastDiff ?: "Medium")
        }
        if (session.winner == "ended" && !gameEndSent && chatRepository != null) {
            gameEndSent = true
            scope.launch {
                runCatching {
                    chatRepository.sendGameEnd(gid!!, "truthdare", "Thanks for playing!")
                }
            }
        }
    }

    // --- Spin logic ---
    fun spinWheel(choice: String) {
        val item = if (choice == "Truth") TRUTHS[Random.nextInt(TRUTHS.size)]
        else DARES[Random.nextInt(DARES.size)]
        currentItem = item
        history = history + (currentTurnName to item)
        spins++

        if (onlineMode && repo != null && gid != null && session != null) {
            val nextTurn = if (myUid == session.p1Uid) session.p2Uid else session.p1Uid
            scope.launch {
                runCatching {
                    repo.makeMove(
                        gid,
                        mapOf(
                            "spins" to spins,
                            "lastChoice" to item.type,
                            "lastText" to item.text,
                            "lastDiff" to item.difficulty,
                            "history" to history.map { (name, tdi) ->
                                mapOf(
                                    "name" to name,
                                    "type" to tdi.type,
                                    "text" to tdi.text,
                                    "difficulty" to tdi.difficulty,
                                )
                            },
                        ),
                        nextTurn,
                        mapOf("choice" to item.type, "by" to myUid),
                    )
                }
            }
        }
    }

    // --- Dismiss mode dialog if navigated from invite ---
    LaunchedEffect(gameId) {
        if (gameId != null) {
            showModeDialog = false
            onlineMode = true
            localMode = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Truth or Dare")
                        val subtitle = when {
                            showModeDialog -> ""
                            onlineMode && waitingForPartner -> "Waiting for partner..."
                            isMyTurn -> "Your turn!"
                            else -> "$currentTurnName's turn"
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!showModeDialog) {
                Text(
                    text = "$currentTurnName's turn",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Round $spins",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                if (onlineMode && waitingForPartner) {
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
                } else {
                    AnimatedContent(
                        targetState = currentItem,
                        transitionSpec = {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                        },
                        label = "card",
                        modifier = Modifier.weight(1f),
                    ) { item ->
                        if (item != null) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.type == "Truth")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = if (item.type == "Truth") "\uD83D\uDC95 TRUTH" else "\uD83D\uDD25 DARE",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (item.type == "Truth")
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        item.text,
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    ) {
                                        Text(
                                            "Difficulty: ${item.difficulty}",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp,
                                            ),
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = "\uD83D\uDC95", fontSize = 64.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Choose Truth or Dare!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (currentItem != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = {
                                currentItem = null
                                if (onlineMode && repo != null && gid != null && session != null) {
                                    val nextTurn = if (myUid == session.p1Uid) session.p2Uid else session.p1Uid
                                    scope.launch {
                                        runCatching {
                                            repo.makeMove(
                                                gid,
                                                mapOf(
                                                    "spins" to spins,
                                                    "history" to history.map { (name, tdi) ->
                                                        mapOf(
                                                            "name" to name,
                                                            "type" to tdi.type,
                                                            "text" to tdi.text,
                                                            "difficulty" to tdi.difficulty,
                                                        )
                                                    },
                                                ),
                                                nextTurn,
                                                mapOf("action" to "done", "by" to myUid),
                                            )
                                        }
                                    }
                                } else if (localMode) {
                                    localTurn = if (localTurn == 1) 2 else 1
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Done \u2713")
                        }
                    }
                } else if (!(onlineMode && waitingForPartner)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = { spinWheel("Truth") },
                            modifier = Modifier.weight(1f),
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("\uD83D\uDC95 Truth", fontSize = 18.sp)
                        }
                        Button(
                            onClick = { spinWheel("Dare") },
                            modifier = Modifier.weight(1f),
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        ) {
                            Text("\uD83D\uDD25 Dare", fontSize = 18.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (history.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "History",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(Modifier.height(4.dp))
                            history.takeLast(3).forEach { (name, item) ->
                                Text(
                                    "$name \u2014 ${item.type}: ${item.text.take(40)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
