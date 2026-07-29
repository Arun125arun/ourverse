package com.lovenote.app.stories

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lovenote.app.R
import com.lovenote.app.chat.PhotoEncoder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val STORY_EXPIRY_MILLIS = 24 * 3600 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
    repository: StoryRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stories by repository.activeStories().collectAsState(initial = emptyList())
    var viewingIndex by remember { mutableStateOf<Int?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showCaptionInput by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var captionDraft by remember { mutableStateOf("") }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingPhotoUri = it
            showCaptionInput = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            showCaptionInput = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.story_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreate = !showCreate }) {
                        Icon(Icons.Default.Add, "New story")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (stories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("📸", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.story_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.story_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // Story ring
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.story_your_stories),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        stories.forEachIndexed { index, story ->
                            StoryRingItem(
                                story = story,
                                myUid = repository.myUid,
                                onClick = { viewingIndex = index },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Story cards list
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    items(stories.size) { index ->
                        StoryCard(
                            story = stories[index],
                            myUid = repository.myUid,
                            onReact = { emoji ->
                                scope.launch { runCatching { repository.reactToStory(stories[index].id, emoji) } }
                            },
                        )
                    }
                }
            }

            // Create overlay
            AnimatedVisibility(
                visible = showCreate,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.fillMaxSize(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { showCreate = false }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.story_create_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.story_create_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StoryActionCard(
                                emoji = "📷",
                                label = stringResource(R.string.story_camera),
                                onClick = {
                                    showCreate = false
                                    // TODO: camera capture via TakePicture contract
                                },
                            )
                            StoryActionCard(
                                emoji = "🖼️",
                                label = stringResource(R.string.story_gallery),
                                onClick = {
                                    showCreate = false
                                    photoLauncher.launch("image/*")
                                },
                            )
                        }
                    }
                }
            }

            // Caption input overlay
            AnimatedVisibility(
                visible = showCaptionInput,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(16.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                showCaptionInput = false
                                pendingPhotoUri = null
                            }) {
                                Icon(Icons.Default.Close, "Cancel")
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        pendingPhotoUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = captionDraft,
                            onValueChange = { captionDraft = it },
                            label = { Text(stringResource(R.string.story_caption_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                val uri = pendingPhotoUri ?: return@Button
                                scope.launch {
                                    runCatching {
                                        val photo = PhotoEncoder.encode(context, uri)
                                        repository.addStory(photo, captionDraft, System.currentTimeMillis() + STORY_EXPIRY_MILLIS)
                                    }
                                }
                                showCaptionInput = false
                                pendingPhotoUri = null
                                captionDraft = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.story_share))
                        }
                    }
                }
            }

            // Full-screen viewer
            viewingIndex?.let { idx ->
                if (idx in stories.indices) {
                    StoryFullScreenViewer(
                        story = stories[idx],
                        myUid = repository.myUid,
                        onDismiss = { viewingIndex = null },
                        onReact = { emoji ->
                            scope.launch { runCatching { repository.reactToStory(stories[idx].id, emoji) } }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryRingItem(story: Story, myUid: String, onClick: () -> Unit) {
    val hasReacted = story.myReaction != null
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "ring",
    )
    val borderColor = if (hasReacted) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.tertiary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(3.dp, borderColor, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (story.senderUid == myUid) "Me" else "❤",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        val timeAgo = remember(story.createdAtMillis) {
            val diff = System.currentTimeMillis() - story.createdAtMillis
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            when {
                hours < 1 -> "now"
                hours < 24 -> "${hours}h"
                else -> "${hours / 24}d"
            }
        }
        Text(
            text = timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun StoryCard(story: Story, myUid: String, onReact: (String) -> Unit) {
    val isMine = story.senderUid == myUid
    val timeText = remember(story.createdAtMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(story.createdAtMillis))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column {
            // Photo
            story.photoBase64.let { photo ->
                // In a real app, decode and show. For now, colored placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📸", fontSize = 40.sp)
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMine) "You" else "Partner",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                if (story.caption.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Reaction bar
                Spacer(Modifier.height(8.dp))
                val reactionOptions = listOf("❤️", "😍", "🔥", "😂", "🥰", "✨")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reactionOptions.forEach { emoji ->
                        val isActive = story.myReaction == emoji
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { onReact(emoji) },
                        ) {
                            Text(
                                text = emoji,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                fontSize = 16.sp,
                            )
                        }
                    }
                }

                // Partner reaction
                if (story.partnerReaction != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Partner reacted ${story.partnerReaction}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryFullScreenViewer(story: Story, myUid: String, onDismiss: () -> Unit, onReact: (String) -> Unit) {
    val reactionOptions = listOf("❤️", "😍", "🔥", "😂", "🥰", "✨")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss)
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text("📸", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (story.caption.isNotBlank()) story.caption else "Photo story",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (story.senderUid == myUid) "You · 24h story" else "Partner · 24h story",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium,
            )

            Spacer(Modifier.height(24.dp))

            // Reaction bar
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                reactionOptions.forEach { emoji ->
                    val isActive = story.myReaction == emoji
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                2.dp,
                                if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                                CircleShape,
                            )
                            .clickable {
                                onReact(emoji)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }
        }

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun StoryActionCard(emoji: String, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier
            .size(140.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
