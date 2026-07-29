package com.lovenote.app

import android.app.Application
import com.lovenote.app.notify.Notifier

class LoveNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
    }
}
