package com.lovenote.app.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R
import kotlinx.coroutines.launch

private data class WelcomePage(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val accent: Color,
)

private val pages = listOf(
    WelcomePage(
        emoji = "\uD83D\uDCAC",
        titleRes = R.string.welcome_title_chat,
        subtitleRes = R.string.welcome_subtitle_chat,
        accent = Color(0xFFE53935),
    ),
    WelcomePage(
        emoji = "\uD83C\uDFAE",
        titleRes = R.string.welcome_title_play,
        subtitleRes = R.string.welcome_subtitle_play,
        accent = Color(0xFF6C63FF),
    ),
    WelcomePage(
        emoji = "\uD83D\uDC95",
        titleRes = R.string.welcome_title_sync,
        subtitleRes = R.string.welcome_subtitle_sync,
        accent = Color(0xFFE84393),
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.welcome_tagline),
                fontSize = 13.sp,
                color = Color(0xFF666666),
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(48.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val data = pages[page]
                val scale by animateFloatAsState(
                    targetValue = if (pagerState.currentPage == page) 1f else 0.85f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                    label = "pagerScale",
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = data.emoji,
                        fontSize = 64.sp,
                        modifier = Modifier.size((64f * scale).dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = stringResource(data.titleRes),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = data.accent,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(data.subtitleRes),
                        fontSize = 15.sp,
                        color = Color(0xFFAAAAAA),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFE53935)
                                else Color(0xFF333333),
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLastPage) {
                        onGetStarted()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
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
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(if (isLastPage) R.string.get_started else R.string.next),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (!isLastPage) {
                Text(
                    text = stringResource(R.string.skip),
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier
                        .clickable { onGetStarted() }
                        .padding(8.dp),
                )
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}
