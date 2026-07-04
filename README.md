# OurVerse ❤

A private universe for two people. Pair up once with an invite code, then:

- **Chat** — WhatsApp-style: text, photos (incl. 🔥 view-once), voice notes,
  reactions, read receipts, typing indicator, presence ("Active now")
- **Calls** — voice & video (WebRTC, peer-to-peer) with screen sharing
- **Notes & doodles** — typed or finger-drawn, shown on the partner's
  home-screen widget and live wallpaper
- **Us tab** — daily question (mutual reveal), couple quiz, mood check-in,
  special-date countdowns & milestones, memories timeline, shared to-dos
  with partner reminders
- **Instant notifications** via a foreground listener service (no paid
  push infrastructure)
- Themes (hand-tuned Material 3 palettes + Material You), in-app updates
  with release notes, Crashlytics, account deletion with full data erasure

**Platforms:** Android app (this repo) + web/PWA companion for iPhone at
https://ourverse-98c44.web.app/app/ (source in `hosting/app/`).

## Stack

Kotlin + Jetpack Compose, Glance (widget), WorkManager, WebRTC
(stream-webrtc-android), Firebase free tier (Auth, Firestore, Hosting,
Crashlytics). No custom server; call signaling goes through Firestore.

## Build

```bash
export JAVA_HOME=~/.local/jdk ANDROID_HOME=~/Android/Sdk
./gradlew test            # unit tests
./gradlew assembleRelease # optimized APK (what users get)
```

## Release an update

```bash
# 1) bump versionCode + versionName in app/build.gradle.kts
# 2) add notes to app/src/.../settings/Changelog.kt
bash deploy-update.sh
```

That builds the release APK, uploads it to GitHub Releases
(`Arun125arun/ourverse-releases`), and updates the hosted `version.json`.
Phones update via Settings → Check for updates.

## Admin

- `bash stats.sh` — usage counts (users, couples, messages/day; never content)
- Firebase console: https://console.firebase.google.com/project/ourverse-98c44

## One-time setup on a new machine

JDK 17 + Android SDK (cmdline tools), `firebase-tools` login as the project
owner, GitHub CLI (`~/.local/bin/ghub`) logged in for releases. The debug
keystore signs all builds — keep `~/.android/debug.keystore` backed up, its
SHA-1 is registered in Firebase and updates must match the installed signature.
