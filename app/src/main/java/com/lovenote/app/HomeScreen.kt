package com.lovenote.app

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovenote.app.call.CallManager
import com.lovenote.app.call.CallOverlay
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.chat.ChatViewModel
import com.lovenote.app.R
import com.lovenote.app.chat.ChatScreen
import com.lovenote.app.notify.AppVisibility
import com.lovenote.app.notify.Notifier
import com.lovenote.app.notify.NotifyState
import com.lovenote.app.settings.SettingsScreen
import com.lovenote.app.ping.PingScreen
import com.lovenote.app.stories.StoryRepository
import com.lovenote.app.stories.StoryScreen
import com.lovenote.app.us.MemoriesScreen
import com.lovenote.app.us.TodosScreen
import com.lovenote.app.us.UsRepository
import com.lovenote.app.us.UsScreen
import com.lovenote.app.us.UsViewModel
import com.lovenote.app.vibe.VibeRepository
import com.lovenote.app.vibe.VibeScreen
import com.lovenote.app.vibe.VibeViewModel
import com.lovenote.app.rituals.RitualDetailScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class HomeScreen {
    CHAT, US, MEMORIES, TODOS, SETTINGS,
    STORY, PING,
    VIBE, RITUAL_DETAIL,
}

@Composable
internal fun Home(coupleId: String, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val chatRepository = remember(coupleId) { ChatRepository(coupleId) }
    val usRepository = remember(coupleId) { UsRepository(coupleId) }
    val vibeRepository = remember(coupleId) { VibeRepository(coupleId) }
    val storyRepository = remember(coupleId) { StoryRepository(coupleId) }
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(app, chatRepository) as T
        },
    )
    val usViewModel: UsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UsViewModel(usRepository) as T
        },
    )
    val vibeViewModel: VibeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VibeViewModel(vibeRepository) as T
        },
    )
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
                            newest.isPhoto -> context.getString(R.string.notification_photo)
                            newest.isVoice -> context.getString(R.string.notification_voice_note)
                            else -> newest.body
                        },
                    )
                }
            }
            NotifyState.setLastMessage(context, millis)
            firstEmission = false
        }
    }

    val partnerName = partner?.name?.ifBlank { stringResource(R.string.default_name_partner) } ?: stringResource(R.string.default_name_partner)
    var selectedRitualId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                            label = stringResource(R.string.tab_chat),
                            selected = screen == HomeScreen.CHAT || screen == HomeScreen.SETTINGS,
                            onClick = { navigate(HomeScreen.CHAT) },
                        )
                        BottomBarItem(
                            icon = Icons.Filled.Favorite,
                            label = stringResource(R.string.tab_us),
                            selected = screen == HomeScreen.US || screen == HomeScreen.MEMORIES ||
                                screen == HomeScreen.TODOS,
                            onClick = { navigate(HomeScreen.US) },
                        )
                        BottomBarItem(
                            icon = Icons.Filled.Add,
                            label = stringResource(R.string.tab_story),
                            selected = screen == HomeScreen.STORY,
                            onClick = { navigate(HomeScreen.STORY) },
                        )
                        BottomBarItem(
                            icon = Icons.Filled.Star,
                            label = stringResource(R.string.tab_vibe),
                            selected = screen == HomeScreen.VIBE || screen == HomeScreen.RITUAL_DETAIL,
                            onClick = { navigate(HomeScreen.VIBE) },
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
                Crossfade(
                    targetState = screen,
                    label = "screens",
                    animationSpec = tween(durationMillis = 250),
                ) { target ->
                    when (target) {
                        HomeScreen.SETTINGS -> SettingsScreen(
                            onBack = { navigate(HomeScreen.CHAT) },
                            onLoggedOut = onLoggedOut,
                            chatRepository = chatRepository,
                        )
                        HomeScreen.US -> UsScreen(
                            vm = usViewModel,
                            onMemoriesClick = { navigate(HomeScreen.MEMORIES) },
                            onTodosClick = { navigate(HomeScreen.TODOS) },
                            onPingClick = { navigate(HomeScreen.PING) },
                        )
                        HomeScreen.STORY -> StoryScreen(
                            repository = storyRepository,
                            onBack = { navigate(HomeScreen.CHAT) },
                        )
                        HomeScreen.PING -> PingScreen(
                            repository = usRepository,
                            onBack = { navigate(HomeScreen.US) },
                        )
                        HomeScreen.MEMORIES -> MemoriesScreen(
                            repository = usRepository,
                            onBack = { navigate(HomeScreen.US) },
                        )
                        HomeScreen.TODOS -> TodosScreen(
                            repository = usRepository,
                            onBack = { navigate(HomeScreen.US) },
                            onRemind = { title ->
                                scope.launch { runCatching { chatRepository.send(context.getString(R.string.notification_reminder, title)) } }
                            },
                        )
                        HomeScreen.CHAT -> ChatScreen(
                            vm = chatViewModel,
                            onSettingsClick = { navigate(HomeScreen.SETTINGS) },
                        )
                        HomeScreen.VIBE -> VibeScreen(
                            vm = vibeViewModel,
                            onRitualDetail = { id ->
                                selectedRitualId = id
                                navigate(HomeScreen.RITUAL_DETAIL)
                            },
                            onShareSong = { uri, source, title, artist, albumArt, audioUrl ->
                                scope.launch { runCatching { vibeViewModel.shareSong(uri, source, title, artist, albumArt, audioUrl) } }
                            },
                        )
                        HomeScreen.RITUAL_DETAIL -> RitualDetailScreen(
                            repository = vibeRepository,
                            ritualId = selectedRitualId ?: "",
                            onBack = { navigate(HomeScreen.VIBE) },
                        )
                    }
                }
            }
        }
        CallOverlay(
            partnerName = partner?.name ?: stringResource(R.string.default_name_partner),
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
