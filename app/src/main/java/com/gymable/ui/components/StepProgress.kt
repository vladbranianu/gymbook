package com.gymable.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Slim animated step indicator. `currentStep` is 1-based.
 */
@Composable
fun StepProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (step in 1..totalSteps) {
            StepSegment(active = step <= currentStep, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.StepSegment(active: Boolean, modifier: Modifier) {
    val color by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "stepColor"
    )
    val barHeight by animateDpAsState(if (active) 6.dp else 4.dp, label = "stepHeight")
    Box(
        modifier = modifier
            .height(barHeight)
            .background(color, CircleShape)
    )
}
