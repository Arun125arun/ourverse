package com.lovenote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.lovenote.app.auth.SignInScreen
import com.lovenote.app.pairing.CoupleStatus
import com.lovenote.app.pairing.PairingRepository
import com.lovenote.app.pairing.PairingScreen
import com.lovenote.app.ui.theme.LoveNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoveNoteTheme {
                var signedIn by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }
                if (!signedIn) {
                    SignInScreen(onSignedIn = { signedIn = true })
                } else {
                    PairedGate()
                }
            }
        }
    }
}

/** Routes between pairing and the main app based on live couple status. */
@Composable
private fun PairedGate() {
    val repository = remember { PairingRepository() }
    val status by remember { repository.observeStatus() }
        .collectAsState(initial = null)

    when {
        status == null -> LoadingScreen()
        status?.coupleId == null || status?.partnerJoined == false ->
            PairingScreen(
                waitingCode = status?.takeIf { it.coupleId != null }?.inviteCode,
                repository = repository,
            )
        else -> HomePlaceholder(status!!)
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// Replaced by the chat screen in the next task.
@Composable
private fun HomePlaceholder(status: CoupleStatus) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Paired! ❤",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
