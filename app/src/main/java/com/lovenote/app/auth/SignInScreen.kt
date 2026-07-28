package com.lovenote.app.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.lovenote.app.R
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(onSignedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val heartbeat by rememberInfiniteTransition(label = "logo")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "logoScale",
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.content_description_ourverse_logo),
                    tint = Color(0xFFE53935),
                    modifier = Modifier
                        .size(48.dp)
                        .scale(heartbeat),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
            )
            Text(
                text = stringResource(R.string.signin_tagline),
                fontSize = 14.sp,
                color = Color(0xFF666666),
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(64.dp))

            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = Color(0xFFE53935),
                    strokeWidth = 3.dp,
                )
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                AuthRepository().signInWithGoogle(context)
                                onSignedIn()
                            } catch (_: GetCredentialCancellationException) {
                            } catch (e: Exception) {
                                error = e.message ?: context.getString(R.string.signin_error_fallback)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(R.string.content_description_google_account),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.continue_with_google),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            error?.let {
                Text(
                    text = it,
                    color = Color(0xFFE53935),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.terms_privacy),
                fontSize = 11.sp,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                maxLines = 2,
            )
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun SignInScreenPreview() {
    com.lovenote.app.ui.theme.LoveNoteTheme {
        SignInScreen(onSignedIn = {})
    }
}
