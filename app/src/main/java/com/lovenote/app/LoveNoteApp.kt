package com.lovenote.app

import android.app.Application
import com.lovenote.app.widget.WidgetRefreshWorker

class LoveNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetRefreshWorker.schedule(this)
        WidgetRefreshWorker.refreshNow(this)
    }
}
