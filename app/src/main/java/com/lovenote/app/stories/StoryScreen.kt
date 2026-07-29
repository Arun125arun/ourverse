package com.lovenote.app.stories

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    var viewingIndex by remember { mutableIntStateOf(-1) }
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
                    IconButton(onClick = { showCreate = true }) {
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
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "No stories yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Share a photo moment with your partner.\nIt disappears after 24 hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showCreate = true },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share a moment")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            stories.forEachIndexed { index, story ->
                                StoryRingItem(
                                    story = story,
                                    myUid = repository.myUid,
                                    onClick = { viewingIndex = index },
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    itemsIndexed(stories) { index, story ->
                        StoryCard(
                            story = story,
                            myUid = repository.myUid,
                            onReact = { emoji ->
                                scope.launch { runCatching { repository.reactToStory(stories[index].id, emoji) } }
                            },
                            onTap = { viewingIndex = index },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "New story",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(onClick = { showCreate = false }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = "Capture or pick a photo.\nIt vanishes after 24 hours.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(40.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Button(
                                onClick = { showCreate = false; photoLauncher.launch("image/*") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Gallery", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Pick from photos",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    showCreate = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Camera", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Capture now",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                            .padding(24.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Add a caption",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(onClick = {
                                showCaptionInput = false
                                pendingPhotoUri = null
                            }) {
                                Icon(Icons.Default.Close, "Cancel")
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        pendingPhotoUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = captionDraft,
                            onValueChange = { captionDraft = it },
                            label = { Text("Write a caption...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
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
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Share story")
                        }
                    }
                }
            }

            if (viewingIndex in stories.indices) {
                StoryFullScreenViewer(
                    story = stories[viewingIndex],
                    myUid = repository.myUid,
                    onDismiss = { viewingIndex = -1 },
                    onReact = { emoji ->
                        scope.launch { runCatching { repository.reactToStory(stories[viewingIndex].id, emoji) } }
                    },
                )
            }
        }
    }
}

@Composable
private fun StoryRingItem(story: Story, myUid: String, onClick: () -> Unit) {
    val hasReacted = story.myReaction != null
    val bitmap = remember(story.photoBase64) {
        runCatching {
            val bytes = Base64.decode(story.photoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    val ringColor = if (hasReacted) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        listOf(ringColor, ringColor.copy(alpha = 0.3f), ringColor)
                    ),
                    shape = CircleShape,
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = (story.senderUid.firstOrNull()?.uppercase() ?: "?"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
            text = if (story.senderUid == myUid) "You" else timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun StoryCard(story: Story, myUid: String, onReact: (String) -> Unit, onTap: () -> Unit) {
    val isMine = story.senderUid == myUid
    val timeText = remember(story.createdAtMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(story.createdAtMillis))
    }
    val bitmap = remember(story.photoBase64) {
        runCatching {
            val bytes = Base64.decode(story.photoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        modifier = Modifier.clickable(onClick = onTap),
    ) {
        Column {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = story.senderUid.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMine) "You" else "Partner",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                if (story.caption.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(12.dp))
                val reactionOptions = listOf("❤️", "😍", "🔥", "😂", "🥰", "✨")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reactionOptions.forEach { emoji ->
                        val isActive = story.myReaction == emoji
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color(0x00000000)
                                )
                                .clickable { onReact(emoji) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Text(emoji, fontSize = 16.sp)
                        }
                    }
                }

                if (story.partnerReaction != null) {
                    Spacer(Modifier.height(8.dp))
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
    val bitmap = remember(story.photoBase64) {
        runCatching {
            val bytes = Base64.decode(story.photoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
            .statusBarsPadding(),
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.3f), Color(0x00000000), Color.Black.copy(alpha = 0.4f))
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (story.senderUid == myUid) "You" else "Partner",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (story.caption.isNotBlank()) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                reactionOptions.forEach { emoji ->
                    val isActive = story.myReaction == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color.White.copy(alpha = 0.25f)
                                else Color(0x00000000)
                            )
                            .clickable { onReact(emoji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
