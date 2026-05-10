package com.campusconnect.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusconnect.app.R
import com.campusconnect.app.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")

    // Entry animations
    val logoScale  = remember { Animatable(0.65f) }
    val logoAlpha  = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subAlpha   = remember { Animatable(0f) }
    val dotsAlpha  = remember { Animatable(0f) }

    // Continuous logo pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.055f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    // Decorative ring slow rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            tween(18000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    LaunchedEffect(Unit) {
        // Staggered entry sequence
        launch {
            logoScale.animateTo(
                targetValue   = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
        }
        launch { logoAlpha.animateTo(1f, tween(700)) }
        launch {
            delay(350)
            titleAlpha.animateTo(1f, tween(600))
        }
        launch {
            delay(600)
            subAlpha.animateTo(1f, tween(600))
        }
        launch {
            delay(900)
            dotsAlpha.animateTo(1f, tween(500))
        }

        // Navigate after splash duration
        delay(2400)
        if (vm.isLoggedIn) onNavigateToDashboard() else onNavigateToAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF070E2B),
                        0.30f to Color(0xFF0D1B45),
                        0.65f to Color(0xFF1E3A8A),
                        1.00f to Color(0xFF2563EB)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── Decorative background rings ────────────────────────────────────
        Box(
            modifier = Modifier
                .size(340.dp)
                .alpha(0.08f)
                .clip(CircleShape)
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .alpha(0.07f)
                .clip(CircleShape)
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .size(195.dp)
                .alpha(0.06f)
                .clip(CircleShape)
                .background(Color.White)
        )

        // ── Content column ─────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        ) {
            // Logo with radial glow + border ring
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.00f)
                                )
                            )
                        )
                )
                // Border ring
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner frosted container
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter            = painterResource(id = R.drawable.campus_logo),
                            contentDescription = "Campus Connect Logo",
                            // No CircleShape clip on the image — parent Box handles clipping.
                            // Fit displays the full transparent logo perfectly within the circle.
                            modifier           = Modifier.size(108.dp),
                            contentScale       = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            // Title — fades in first
            Text(
                text          = "Campus Connect",
                fontSize      = 36.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = Color.White,
                textAlign     = TextAlign.Center,
                letterSpacing = (-0.5).sp,
                modifier      = Modifier.alpha(titleAlpha.value)
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle — fades in after title
            Text(
                text          = "Your Student Hub",
                fontSize      = 15.sp,
                fontWeight    = FontWeight.Medium,
                color         = Color.White.copy(alpha = 0.65f),
                textAlign     = TextAlign.Center,
                letterSpacing = 1.2.sp,
                modifier      = Modifier.alpha(subAlpha.value)
            )

            Spacer(Modifier.height(72.dp))

            // Animated loading dots (alpha + size pulse)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.alpha(dotsAlpha.value)
            ) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue  = 0.25f,
                        targetValue   = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(550, delayMillis = index * 180, easing = EaseInOut),
                            RepeatMode.Reverse
                        ),
                        label = "dot_alpha_$index"
                    )
                    val dotSize by infiniteTransition.animateFloat(
                        initialValue  = 7f,
                        targetValue   = 10f,
                        animationSpec = infiniteRepeatable(
                            tween(550, delayMillis = index * 180, easing = EaseInOut),
                            RepeatMode.Reverse
                        ),
                        label = "dot_size_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}