package com.gymable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.SurfaceElevated
import com.gymable.ui.theme.TextSecondary
import com.gymable.ui.tutorial.TutorialState

private data class StepContent(val title: String, val description: String)

private val STEPS = listOf(
    StepContent(
        title = "Start Workout",
        description = "Tap here to begin a session. Pick your gym, muscle group, and exercise, then log your sets."
    ),
    StepContent(
        title = "Stats",
        description = "Track your progress over time — weight lifted, reps, and total volume per exercise."
    ),
    StepContent(
        title = "History",
        description = "Browse all your past workout sessions, grouped by day."
    ),
    StepContent(
        title = "Exercises",
        description = "View all your exercises and check the last results at a glance."
    ),
    StepContent(
        title = "Add Exercise",
        description = "Create a new exercise under any gym and muscle group."
    ),
    StepContent(
        title = "Settings",
        description = "Export your data as a PDF report or backup file. Restore anytime with Import."
    )
)

private val SPOTLIGHT_PADDING = 10.dp
private val TOOLTIP_MARGIN = 16.dp
private val TOOLTIP_GAP = 12.dp
private val TOTAL_STEPS = STEPS.size

@Composable
fun TutorialOverlay(
    state: TutorialState,
    bounds: Map<Int, Rect>,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    AnimatedVisibility(
        visible = state != TutorialState.Hidden,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        when (state) {
            TutorialState.Hidden -> Unit
            TutorialState.Final -> FinalOverlay(onNext = onNext)
            is TutorialState.Step -> StepOverlay(
                stepIndex = state.index,
                spotlightBounds = bounds[state.index],
                content = STEPS.getOrNull(state.index) ?: return@AnimatedVisibility,
                onNext = onNext,
                onSkip = onSkip
            )
        }
    }
}

@Composable
private fun StepOverlay(
    stepIndex: Int,
    spotlightBounds: Rect?,
    content: StepContent,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val spotPadPx = with(density) { SPOTLIGHT_PADDING.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim with spotlight cutout — separate Canvas so tooltip draws on top
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            spotlightBounds?.let { r ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(r.left - spotPadPx, r.top - spotPadPx),
                    size = Size(r.width + spotPadPx * 2, r.height + spotPadPx * 2),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // Tooltip positioned below the spotlight (or centered if bounds unknown)
        if (spotlightBounds != null) {
            val tooltipTopDp = with(density) {
                (spotlightBounds.bottom + spotPadPx + TOOLTIP_GAP.toPx()).toDp()
            }
            TooltipCard(
                content = content,
                stepIndex = stepIndex,
                totalSteps = TOTAL_STEPS,
                onNext = onNext,
                onSkip = onSkip,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = TOOLTIP_MARGIN,
                        end = TOOLTIP_MARGIN,
                        top = tooltipTopDp
                    )
            )
        } else {
            TooltipCard(
                content = content,
                stepIndex = stepIndex,
                totalSteps = TOTAL_STEPS,
                onNext = onNext,
                onSkip = onSkip,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = TOOLTIP_MARGIN)
            )
        }
    }
}

@Composable
private fun TooltipCard(
    content: StepContent,
    stepIndex: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stepIndex + 1} / $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Text(
                text = content.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                PrimaryButton(
                    text = if (stepIndex == totalSteps - 1) "Almost done" else "Next",
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun FinalOverlay(onNext: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.82f))
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceElevated,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💪",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "Enjoy your workout!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "You're all set. Track every set and beat your records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "Let's go!",
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
