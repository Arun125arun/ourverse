# ── WebRTC (org.webrtc / Stream SDK) ──────────────────────────────────────────
# JNI bridge — native methods called via reflection
-keep class org.webrtc.** { *; }
-keep class io.getstream.** { *; }
-dontwarn io.getstream.**

# ── Media3 / ExoPlayer ───────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Firebase / Crashlytics ───────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
# Crashlytics mapping file upload
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ── Glance (App Widgets) ─────────────────────────────────────────────────────
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# ── Credentials / Google Identity ────────────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Compose ──────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
