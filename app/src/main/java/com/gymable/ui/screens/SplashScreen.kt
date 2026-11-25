package com.gymable.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymable.ui.theme.Background
import com.gymable.ui.theme.BebasNeue
import com.gymable.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onReady: () -> Unit) {
    val screenAlpha    = remember { Animatable(1f) }
    val logoAlpha      = remember { Animatable(0f) }
    val logoScale      = remember { Animatable(0.82f) }
    val taglineAlpha   = remember { Animatable(0f) }
    val taglineOffset  = remember { Animatable(14f) }
    val scanY          = remember { Animatable(0f) }
    val scanAlpha      = remember { Animatable(0f) }
    val ring1R         = remember { Animatable(0f) }
    val ring1A         = remember { Animatable(0f) }
    val ring2R         = remember { Animatable(0f) }
    val ring2A         = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1 — logo reveal
        launch { logoAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        launch { logoScale.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        delay(150)
        launch { taglineAlpha.animateTo(1f, tween(280)) }
        launch { taglineOffset.animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }

        // Phase 2 — scanner sweep
        delay(30)
        launch {
            scanAlpha.animateTo(1f, tween(60))
            scanY.animateTo(1f, tween(480, easing = LinearEasing))
            scanAlpha.animateTo(0f, tween(150))
        }

        // Phase 3 — rings
        delay(160)
        launch {
            ring1A.animateTo(0.65f, tween(60))
            ring1R.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            ring1A.animateTo(0f, tween(220))
        }
        delay(120)
        launch {
            ring2A.animateTo(0.4f, tween(60))
            ring2R.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            ring2A.animateTo(0f, tween(220))
        }

        // Phase 4 — fade out
        delay(580)
        screenAlpha.animateTo(0f, tween(200, easing = FastOutLinearInEasing))
        onReady()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        val maxW    = constraints.maxWidth.toFloat()
        val maxH    = constraints.maxHeight.toFloat()
        val maxRing = maxW.coerceAtLeast(maxH) * 0.52f
        val accentColor = MaterialTheme.colorScheme.primary

        Canvas(Modifier.fillMaxSize()) {
            // Scanner line with horizontal glow
            val yPx = scanY.value * maxH
            val sA  = scanAlpha.value
            if (sA > 0f) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = sA * 0.35f),
                            accentColor.copy(alpha = sA),
                            accentColor.copy(alpha = sA * 0.35f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, yPx),
                    end   = Offset(maxW, yPx),
                    strokeWidth = 2f
                )
                // trailing glow band below the line
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = sA * 0.10f),
                            Color.Transparent
                        ),
                        startY = yPx,
                        endY   = (yPx + 80f).coerceAtMost(maxH)
                    )
                )
            }

            // Ring 1
            if (ring1A.value > 0f) {
                drawCircle(
                    color  = accentColor.copy(alpha = ring1A.value),
                    radius = ring1R.value * maxRing,
                    center = Offset(maxW / 2f, maxH / 2f),
                    style  = Stroke(width = 1.5f)
                )
            }
            // Ring 2
            if (ring2A.value > 0f) {
                drawCircle(
                    color  = accentColor.copy(alpha = ring2A.value),
                    radius = ring2R.value * maxRing,
                    center = Offset(maxW / 2f, maxH / 2f),
                    style  = Stroke(width = 1f)
                )
            }
        }

        // Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(logoAlpha.value)
                .scale(logoScale.value)
        ) {
            Text(
                text         = "GYM",
                fontFamily   = BebasNeue,
                fontSize     = 72.sp,
                fontWeight   = FontWeight.Normal,
                color        = TextPrimary,
                letterSpacing = 10.sp
            )
            Text(
                text         = "BOOK",
                fontFamily   = BebasNeue,
                fontSize     = 72.sp,
                fontWeight   = FontWeight.Normal,
                color        = accentColor,
                letterSpacing = 10.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text         = "TRAINING EVOLVED",
                fontSize     = 11.sp,
                fontWeight   = FontWeight.Medium,
                color        = accentColor,
                letterSpacing = 4.sp,
                textAlign    = TextAlign.Center,
                modifier     = Modifier
                    .alpha(taglineAlpha.value)
                    .offset(y = taglineOffset.value.dp)
            )
        }
    }
}
