package com.lovenote.app.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.service.wallpaper.WallpaperService
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.toArgb
import com.lovenote.app.notes.Note
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.notes.noteStyleColors

/**
 * Live wallpaper that shows the partner's latest note as a card over a soft
 * background. Reads the same cache the widget uses and redraws whenever the
 * cache changes (in-app listener or the 15-minute background refresh).
 */
class NoteWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = NoteEngine()

    inner class NoteEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs by lazy {
            getSharedPreferences("note_widget_cache", Context.MODE_PRIVATE)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
            if (surfaceHolder.surface.isValid) draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) draw()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        private fun draw() {
            val canvas = surfaceHolder.lockCanvas() ?: return
            try {
                render(canvas)
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        private fun render(canvas: Canvas) {
            val note = NoteCache.load(applicationContext)
            val cardColor =
                (noteStyleColors[note?.style] ?: noteStyleColors.getValue(Note.DEFAULT_STYLE))
                    .toArgb()

            // Soft dark background so the card pops on any launcher.
            canvas.drawColor(Color.rgb(38, 22, 30))

            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val text = note?.text ?: "No note yet — send one! ❤"

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 0, 0, 0)
                textSize = width / 16f
            }
            val cardWidth = width * 0.82f
            val textLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, (cardWidth * 0.85f).toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.2f)
                .build()

            val padding = width * 0.08f
            val cardHeight = textLayout.height + padding * 2
            val card = RectF(
                (width - cardWidth) / 2f,
                (height - cardHeight) / 2f,
                (width + cardWidth) / 2f,
                (height + cardHeight) / 2f,
            )
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cardColor
                setShadowLayer(24f, 0f, 8f, Color.argb(90, 0, 0, 0))
            }
            canvas.drawRoundRect(card, 48f, 48f, cardPaint)

            canvas.save()
            canvas.translate(
                card.left + (cardWidth - textLayout.width) / 2f,
                card.top + padding,
            )
            textLayout.draw(canvas)
            canvas.restore()

            // Small heart under the card
            val heartPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = width / 20f
                textAlign = Paint.Align.CENTER
                color = Color.argb(160, 255, 255, 255)
            }
            canvas.drawText("❤", width / 2f, card.bottom + width / 10f, heartPaint)
        }
    }
}
