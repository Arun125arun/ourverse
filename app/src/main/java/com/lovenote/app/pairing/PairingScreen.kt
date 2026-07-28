package com.lovenote.app.pairing

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.R
import com.lovenote.app.notify.Notifier
import kotlinx.coroutines.launch

/**
 * Pairing flow. When [waitingCode] is non-null the user already created a
 * couple and is waiting for their partner; otherwise offer create/join.
 */
@Composable
fun PairingScreen(
    waitingCode: String?,
    repository: PairingRepository,
    onLoggedOut: () -> Unit = {},
) {
    val context = LocalContext.current
    val logoutScope = rememberCoroutineScope()

    fun logOut() {
        logoutScope.launch {
            runCatching {
                CredentialManager.create(context)
                    .clearCredentialState(ClearCredentialStateRequest())
            }
            FirebaseAuth.getInstance().signOut()
            onLoggedOut()
        }
    }

    if (waitingCode != null) {
        WaitingForPartner(waitingCode, onLogOut = ::logOut)
        return
    }

    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var codeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.create_couple_space),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))

        if (busy) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            repository.createCouple()
                            // observeStatus() flips the UI to the waiting screen
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.get_invite_code))
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.have_partner_code),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it.uppercase().take(InviteCode.LENGTH) },
                label = { Text(stringResource(R.string.partner_code_field)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            repository.joinWithCode(codeInput)
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = codeInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.join_button))
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { logOut() }) {
            Text(stringResource(R.string.log_out_button), color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun WaitingForPartner(code: String, onLogOut: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.send_code_to_partner),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = code,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 8.sp,
            modifier = Modifier.clickable {
                clipboard.setText(AnnotatedString(code))
                copied = true
                Notifier.vibrate(context)
            },
        )
        Text(
            text = if (copied) stringResource(R.string.copied_label) else stringResource(R.string.tap_to_copy),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    context.getString(R.string.share_invite_message, code),
                )
            }
            runCatching {
                context.startActivity(Intent.createChooser(share, context.getString(R.string.share_via)))
            }
        }) {
            Text(stringResource(R.string.share_invite_link))
        }
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.waiting_for_partner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onLogOut) {
            Text(stringResource(R.string.log_out_button), color = MaterialTheme.colorScheme.secondary)
        }
    }
}
