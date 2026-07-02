# LoveNote — Design Spec

**Date:** 2026-07-02
**Status:** Approved by user

## Overview

LoveNote is an Android app for two people in a relationship. Partners pair up
once, then chat with each other and leave short notes that appear on their
partner's phone home screen.

- **Platform:** Android only
- **App stack:** Kotlin + Jetpack Compose (widget via Jetpack Glance)
- **Backend:** Firebase free tier (Auth, Firestore, Storage, Cloud Messaging)
- **Working name:** LoveNote (user may rename later)

## Features

### 1. Pairing

- Sign in with Google (Firebase Auth, one-tap; no password system to build).
- First partner receives a 6-character invite code; the second partner enters
  it to link the two accounts into a couple.
- Exactly one partner per account. Unpairing is out of scope for v1.

### 2. Chat (rich)

One-to-one chat screen between the two partners:

- Text messages with full history.
- Photo messages (stored in Firebase Storage).
- Emoji reactions on individual messages.
- Read receipts (seen ✓✓).
- Typing indicator.
- Push notification on the partner's phone when a message arrives
  (Firebase Cloud Messaging).

### 3. Notes on the home screen

- A "Send a note" screen: type a short message, pick a background
  color/style, send.
- The partner's phone shows the **latest note** in a home-screen widget
  (Jetpack Glance). The widget refreshes within seconds of a new note via an
  FCM data message.
- **Phase 3:** an optional true live wallpaper (Android
  `WallpaperService`) that draws the latest note over a background image for
  partners who want it fullscreen. The widget remains the default.

## Architecture

```
Phone A (Kotlin + Compose)          Phone B (Kotlin + Compose)
   │  ▲                                │  ▲
   ▼  │                                ▼  │
            Firebase (free tier)
            ├─ Auth (Google sign-in)
            ├─ Firestore (couples, messages, notes)
            ├─ Storage (photos)
            └─ Cloud Messaging (push → notifications + widget refresh)
```

No custom server. All delivery is Firestore realtime listeners plus FCM for
background push.

### Data model (Firestore)

- `users/{uid}` — display name, photo, fcmToken, coupleId
- `couples/{coupleId}` — the two member uids, inviteCode, createdAt
- `couples/{coupleId}/messages/{messageId}` — sender, type (text/photo),
  body or storage path, reactions map, sentAt, seenAt
- `couples/{coupleId}/notes/{noteId}` — sender, text, style, sentAt
  (widget shows the newest)
- Typing indicator: a lightweight `typing` field on the couple doc.

Security rules: only the two members of a couple can read/write its data.

## Error handling

- Offline: Firestore's built-in offline cache; messages queue and send when
  back online.
- Invalid/expired invite code: clear inline error, allow retry.
- Widget with no note yet / signed out: friendly placeholder state.
- Photo upload failure: retry affordance on the failed message.

## Testing

- Unit tests for pairing logic, message/note repositories (Firebase emulator
  suite where practical).
- Manual two-device testing for chat realtime behavior, push notifications,
  and widget refresh.

## Build phases

1. **Phase 1 — the heart:** Google sign-in, pairing via invite code, basic
   text chat, note widget. A working app installable on two phones.
2. **Phase 2 — rich chat:** photos, emoji reactions, read receipts, typing
   indicator, push notifications polish.
3. **Phase 3 — live wallpaper** option.

## Prerequisites (user-side)

- Android Studio installed.
- A free Firebase project (created in the browser, ~5 minutes; will be done
  together during setup).
- One or two Android phones for testing (scrcpy already installed).
