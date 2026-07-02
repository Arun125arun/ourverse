package com.lovenote.app.settings

import android.content.Context
import android.os.Build
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Checks the hosted version.json for a newer build of the app. */
object UpdateChecker {
    private const val VERSION_URL = "https://ourverse-98c44.web.app/version.json"

    data class UpdateInfo(
        val versionCode: Long,
        val versionName: String,
        val url: String,
    )

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(URL(VERSION_URL).readText())
            UpdateInfo(
                versionCode = json.getLong("versionCode"),
                versionName = json.getString("versionName"),
                url = json.getString("url"),
            )
        }.getOrNull()
    }

    fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    fun installedVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}
