package com.lovenote.app.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Snapshot listeners lose permission mid-flight when the couple is deleted
 * (account deletion by the partner) or during a rules rollout. Falling back
 * to a neutral value keeps the UI alive while the pairing gate routes back
 * to the pairing screen, instead of crashing the whole composition.
 */
fun <T> Flow<T>.fallbackTo(default: T): Flow<T> = catch { emit(default) }
