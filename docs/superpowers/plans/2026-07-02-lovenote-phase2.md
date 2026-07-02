# LoveNote Phase 2 Implementation Plan — Rich Chat

**Goal:** Photos, emoji reactions, read receipts, and a typing indicator in chat.

**Decisions (user delegated):**
- Firebase Blaze plan skipped for now → no FCM push; instant delivery stays
  Phase 3 material alongside the live wallpaper.
- New Firebase projects require the paid plan for Cloud Storage, so photo
  messages are stored **inside Firestore** as compressed JPEG Base64
  (scaled to ≤1280px, quality ~70, hard cap ~700KB) instead of Storage.
  Fine for a two-person app; keeps everything on the free tier.

## Data model changes

- `messages/{id}` gains:
  - `seenAt: Timestamp?` — set by the *recipient* when they view the chat
  - `reactions: Map<uid, emoji>` — one reaction per person per message
  - `type: "photo"` messages carry Base64 JPEG in `body`
- `couples/{coupleId}` gains `typing.<uid>: Timestamp` — heartbeat while the
  partner is typing; shown if fresher than ~6 seconds.

## Tasks

1. **Message model v2 (TDD):** extend `MessageTest` for `seenAt`,
   `reactions`, photo type round-trip; update `Message.toMap/fromMap`.
2. **Read receipts:** `ChatRepository.markPartnerMessagesSeen()` called when
   the message list is visible/updated; ChatScreen shows "Seen ✓✓" under the
   newest of my seen messages.
3. **Typing indicator:** `ChatRepository.setTyping()` heartbeat while the
   input has text focus/changes; `partnerTyping(): Flow<Boolean>` from the
   couple doc; "typing…" label in the top bar.
4. **Emoji reactions:** long-press a bubble → row of ❤ 😂 😍 😢 👍; stored in
   `reactions`; shown as a small chip on the bubble corner. Tap same emoji
   again to remove.
5. **Photo messages:** photo-picker button in the input row
   (`PickVisualMedia`), downscale + compress to JPEG Base64 with size guard,
   `send` as `type: "photo"`, decode + render in bubbles.

## Verification

`./gradlew test assembleDebug` green after each task; manual two-device
run-through happens together with the Phase 1 checklist once Firebase is
configured.
