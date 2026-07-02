# LoveNote ❤

An Android app for two people. Pair up once with an invite code, then chat
and leave notes that appear on your partner's home screen widget.

## Tech

- Kotlin + Jetpack Compose, Jetpack Glance (widget), WorkManager
- Firebase: Auth (Google sign-in), Cloud Firestore
- No custom server

## One-time setup

1. **Firebase** (in a browser, free tier):
   - Create a project at console.firebase.google.com
   - Add an Android app: package `com.lovenote.app`, plus your debug SHA-1
     (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`)
   - Download `google-services.json` into `app/`
   - Enable **Authentication → Google** provider
   - Create a **Firestore** database (production mode) and publish the rules
     from `firestore.rules`
2. **Toolchain**: JDK 17 and the Android SDK (this repo was built with
   command-line tools; Android Studio also works).

## Build & install

```bash
./gradlew test           # unit tests
./gradlew assembleDebug  # build APK
./gradlew installDebug   # install on a phone connected via adb
```

To install on the second phone (your partner's): enable Developer Options →
USB debugging, plug it in, and run `./gradlew installDebug` — or just send
them `app/build/outputs/apk/debug/app-debug.apk` and open it on the phone.

> Note: the debug APK is signed with *this computer's* debug key. Both
> partners should use APKs built from the same machine, and that machine's
> SHA-1 must be registered in Firebase for Google sign-in to work.

## Using the app

1. Both partners sign in with Google.
2. One partner taps **Get an invite code** and shares the 6-character code.
3. The other partner enters it and taps **Join**.
4. Chat away. Tap the pencil icon to send a note — it shows up on your
   partner's **LoveNote widget** (long-press the home screen → Widgets →
   LoveNote to add it).

The widget updates instantly while the app is open, and on a ~15-minute
schedule in the background. Instant background push is planned (requires
Firebase Blaze plan).
