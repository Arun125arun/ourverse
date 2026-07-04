package com.lovenote.app.widget

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lovenote.app.MainActivity
import com.lovenote.app.notes.Note
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.notes.noteStyleColors

class NoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val note = NoteCache.load(context)
        val doodle = note?.doodle?.let {
            runCatching {
                val bytes = android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
        provideContent { WidgetContent(note, doodle) }
    }

    @Composable
    private fun WidgetContent(note: Note?, doodle: android.graphics.Bitmap?) {
        val background = noteStyleColors[note?.style] ?: noteStyleColors.getValue(Note.DEFAULT_STYLE)
        if (doodle != null) {
            androidx.glance.Image(
                provider = androidx.glance.ImageProvider(doodle),
                contentDescription = "Doodle from your partner",
                contentScale = androidx.glance.layout.ContentScale.Crop,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            )
            return
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(background)
                .cornerRadius(20.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (note == null) {
                Text(
                    text = "No note yet — send one! ❤",
                    style = TextStyle(
                        color = ColorProvider(Color(0xB3000000)),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            } else {
                Text(
                    text = note.text,
                    style = TextStyle(
                        color = ColorProvider(Color(0xE6000000)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "❤ " + relativeTime(note),
                    style = TextStyle(
                        color = ColorProvider(Color(0x8A000000)),
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }

    private fun relativeTime(note: Note): String {
        val millis = note.sentAt?.toDate()?.time ?: return "just now"
        return DateUtils.getRelativeTimeSpanString(millis).toString()
    }
}
