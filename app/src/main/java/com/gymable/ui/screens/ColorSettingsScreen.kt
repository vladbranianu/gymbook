package com.gymable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.theme.AccentOption
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.TextSecondary
import com.gymable.ui.theme.ThemeViewModel

@Composable
fun ColorSettingsScreen(
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    onDonation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected by themeViewModel.selectedAccent.collectAsStateWithLifecycle()
    val maxDonationTier by themeViewModel.maxDonationTier.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "Accent Color",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AccentOption.values().forEach { option ->
                val locked = option.requiredTier > maxDonationTier
                ColorOptionRow(
                    option = option,
                    isSelected = option == selected,
                    locked = locked,
                    onClick = {
                        if (locked) onDonation() else themeViewModel.setAccent(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorOptionRow(
    option: AccentOption,
    isSelected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isSelected) Modifier.border(2.dp, option.accent, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (locked) option.accent.copy(alpha = 0.35f) else option.accent,
                    modifier = Modifier.size(if (isSelected) 30.dp else 36.dp)
                ) {}
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (locked) TextSecondary else MaterialTheme.colorScheme.onSurface
                )
                if (locked) {
                    Text(
                        text = when (option.requiredTier) {
                            1 -> "Unlock with One Scoop ☕"
                            2 -> "Unlock with Gym Chalk 🧴"
                            3 -> "Unlock with Recovery Shake 🏆"
                            else -> "Locked"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            when {
                locked -> Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                isSelected -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = option.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
