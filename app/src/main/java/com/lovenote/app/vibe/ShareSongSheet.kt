package com.lovenote.app.vibe

import com.lovenote.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSongSheet(
    onDismiss: () -> Unit,
    onShare: (uri: String, source: String, title: String, artist: String, albumArtUrl: String?, audioUrl: String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(0) }
    var uriInput by remember { mutableStateOf("") }
    var manualTitle by remember { mutableStateOf("") }
    var manualArtist by remember { mutableStateOf("") }
    var fetchedTitle by remember { mutableStateOf<String?>(null) }
    var fetchedArtist by remember { mutableStateOf<String?>(null) }
    var fetchedAlbumArt by remember { mutableStateOf<String?>(null) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var audioLink by remember { mutableStateOf("") }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.share_song_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == 0,
                    onClick = { mode = 0; fetchError = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.share_paste_link)) }
                SegmentedButton(
                    selected = mode == 1,
                    onClick = { mode = 1; fetchError = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.share_enter_manually)) }
            }

            Spacer(Modifier.height(16.dp))

            if (mode == 0) {
                OutlinedTextField(
                    value = uriInput,
                    onValueChange = { uriInput = it; fetchError = null },
                    label = { Text(stringResource(R.string.share_link_label)) },
                    placeholder = { Text(stringResource(R.string.share_link_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))

                val error = fetchError
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (isFetching) {
                    Text(
                        text = stringResource(R.string.share_fetching),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                if (fetchedTitle != null || fetchedArtist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = fetchedTitle ?: stringResource(R.string.unknown),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (fetchedArtist != null) {
                            Text(
                                text = stringResource(R.string.by_artist, fetchedArtist!!),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (uriInput.isBlank()) return@Button
                        scope.launch {
                            isFetching = true
                            fetchError = null
                            val result = withContext(Dispatchers.IO) {
                                runCatching { fetchTrackMetadata(uriInput.trim()) }.getOrNull()
                            }
                            if (result != null) {
                                fetchedTitle = result.first
                                fetchedArtist = result.second
                                fetchedAlbumArt = result.third
                            } else {
                                fetchError = context.getString(R.string.share_fetch_error)
                            }
                            isFetching = false
                        }
                    },
                    enabled = uriInput.isNotBlank() && !isFetching,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.share_fetch_details)) }

                Spacer(Modifier.height(8.dp))

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = audioLink,
                    onValueChange = { audioLink = it },
                    label = { Text(stringResource(R.string.share_audio_link_label)) },
                    placeholder = { Text(stringResource(R.string.share_audio_link_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.share_audio_link_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val title = fetchedTitle ?: manualTitle.ifBlank { "Unknown" }
                        val artist = fetchedArtist ?: "Unknown"
                        onShare(uriInput.trim(), "spotify", title, artist, fetchedAlbumArt, audioLink.trim().ifBlank { null })
                    },
                    enabled = uriInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) { Text(stringResource(R.string.share_anyway)) }
            } else {
                OutlinedTextField(
                    value = manualTitle,
                    onValueChange = { manualTitle = it },
                    label = { Text(stringResource(R.string.share_song_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualArtist,
                    onValueChange = { manualArtist = it },
                    label = { Text(stringResource(R.string.share_artist_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = audioLink,
                    onValueChange = { audioLink = it },
                    label = { Text(stringResource(R.string.share_audio_link_label)) },
                    placeholder = { Text(stringResource(R.string.share_audio_link_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.share_audio_link_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val title = manualTitle.trim().ifBlank { "Unknown" }
                        val artist = manualArtist.trim().ifBlank { "Unknown" }
                        onShare("manual", "manual", title, artist, null, audioLink.trim().ifBlank { null })
                    },
                    enabled = manualTitle.isNotBlank() || manualArtist.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.share_button)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun fetchTrackMetadata(uri: String): Triple<String, String, String?>? {
    val trackId = extractTrackId(uri) ?: return null
    return try {
        val oembedUrl = "https://open.spotify.com/oembed?url=spotify:track:$trackId"
        val conn = URL(oembedUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        val json = conn.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val title = obj.optString("title", null) ?: return null
        val artist = obj.optString("author_name", "Unknown")
        val albumArt = obj.optString("thumbnail_url", null)
        Triple(title, artist, albumArt)
    } catch (_: Exception) { null }
}

internal fun extractTrackId(uri: String): String? {
    val spotifyPattern = Regex("""(?:spotify:track:|https?://open\.spotify\.com/track/)([a-zA-Z0-9]+)""")
    val youtubePattern = Regex("""(?:https?://(?:www\.)?youtu\.be/|https?://(?:www\.)?youtube\.com/watch\?v=)([a-zA-Z0-9_-]+)""")
    return spotifyPattern.find(uri)?.groupValues?.get(1)
        ?: youtubePattern.find(uri)?.groupValues?.get(1)
}
