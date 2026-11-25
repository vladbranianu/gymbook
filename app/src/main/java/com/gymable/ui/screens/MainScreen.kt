package com.gymable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.data.local.dao.RecentRecordRow
import com.gymable.data.local.dao.WorkoutHistoryRow
import com.gymable.ui.components.SectionLabel
import com.gymable.ui.components.TutorialOverlay
import com.gymable.ui.main.MainViewModel
import com.gymable.ui.main.WeeklyActivityState
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.Outline
import com.gymable.ui.theme.RecordGold
import com.gymable.ui.theme.TextDisabled
import com.gymable.ui.theme.TextPrimary
import com.gymable.ui.theme.TextSecondary
import com.gymable.ui.theme.fmtWeight
import com.gymable.ui.tutorial.TutorialViewModel

@Composable
fun MainScreen(
    onNewWorkout: () -> Unit,
    onContinueWorkout: () -> Unit,
    isWorkoutActive: Boolean,
    onAddExercise: () -> Unit,
    onMyExercises: () -> Unit,
    onStats: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onSupportDev: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    tutorialViewModel: TutorialViewModel = hiltViewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val maxDonationTier by viewModel.maxDonationTier.collectAsStateWithLifecycle()
    val tutorialState by tutorialViewModel.state.collectAsStateWithLifecycle()
    val tutorialBounds = remember { mutableStateMapOf<Int, Rect>() }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.size(4.dp))

                HeaderSection(
                    onSettings = onSettings,
                    donorBadge = when (maxDonationTier) {
                        1 -> "Supporter"
                        2 -> "Gym Rat"
                        3 -> "Legend"
                        else -> null
                    },
                    onSettingsBoundsChanged = { tutorialBounds[5] = it }
                )

                StartWorkoutCard(
                    lastSession = dashboardState.lastSession,
                    isWorkoutActive = isWorkoutActive,
                    onClick = if (isWorkoutActive) onContinueWorkout else onNewWorkout,
                    modifier = Modifier.onGloballyPositioned { tutorialBounds[0] = it.boundsInWindow() }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        icon = Icons.Rounded.BarChart,
                        title = "Stats",
                        subtitle = "${dashboardState.monthlySessionCount} sessions this month",
                        onClick = onStats,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { tutorialBounds[1] = it.boundsInWindow() }
                    )
                    NavCard(
                        icon = Icons.Rounded.History,
                        title = "History",
                        subtitle = "This month",
                        onClick = onHistory,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { tutorialBounds[2] = it.boundsInWindow() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        icon = Icons.Rounded.FitnessCenter,
                        title = "Exercises",
                        subtitle = null,
                        onClick = onMyExercises,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { tutorialBounds[3] = it.boundsInWindow() }
                    )
                    NavCard(
                        icon = Icons.Rounded.Add,
                        title = "Add Exercise",
                        subtitle = null,
                        onClick = onAddExercise,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { tutorialBounds[4] = it.boundsInWindow() }
                    )
                }

                if (dashboardState.topRecords.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Personal Records")
                        dashboardState.topRecords.forEach { record ->
                            RecordRow(record = record)
                        }
                    }
                }

                WeeklyActivitySection(state = dashboardState.weeklyActivity)

                TextButton(
                    onClick = onSupportDev,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Support the Developer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))
            }
        }

        TutorialOverlay(
            state = tutorialState,
            bounds = tutorialBounds,
            onNext = tutorialViewModel::advance,
            onSkip = tutorialViewModel::skip
        )
    }
}

@Composable
private fun HeaderSection(
    onSettings: () -> Unit,
    donorBadge: String? = null,
    onSettingsBoundsChanged: (Rect) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "GYMBOOK",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Track every set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (donorBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = donorBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier.onGloballyPositioned { onSettingsBoundsChanged(it.boundsInWindow()) }
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StartWorkoutCard(
    lastSession: WorkoutHistoryRow?,
    isWorkoutActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = 0.30f),
                spotColor = accentColor.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp)
            )
            Column {
                Text(
                    text = if (isWorkoutActive) "CONTINUE WORKOUT" else "START WORKOUT",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = if (isWorkoutActive) {
                        "Workout in progress"
                    } else if (lastSession != null) {
                        "${lastSession.exerciseName} · ${lastSession.gymName} · ${formatRelativeDate(lastSession.date)}"
                    } else {
                        "Start your first workout"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun NavCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecordRow(record: RecentRecordRow) {
    val unit = LocalUnitSystem.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = record.exerciseName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = record.maxWeight.fmtWeight(unit),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Icon(
            imageVector = Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = RecordGold,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        )
    }
}

@Composable
private fun WeeklyActivitySection(state: WeeklyActivityState) {
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("This Week")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, label ->
                        DayDot(label = label, active = i in state.activeDays)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    WeeklyStat(value = "${state.sessionCount}", label = "sessions")
                    if (state.totalVolumeKg > 0f) {
                        WeeklyStat(value = formatVolume(state.totalVolumeKg), label = "total volume")
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(label: String, active: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (active) primary else MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) primary else TextDisabled
        )
    }
}

@Composable
private fun WeeklyStat(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

private fun formatVolume(kg: Float): String =
    if (kg >= 1000f) "${"%.1f".format(kg / 1000f)}t" else "${kg.toInt()}kg"

private fun formatRelativeDate(dateMs: Long): String {
    val diffMs = System.currentTimeMillis() - dateMs
    val diffDays = (diffMs / 86_400_000L).toInt()
    return when {
        diffDays == 0 -> "today"
        diffDays == 1 -> "yesterday"
        diffDays < 7 -> "${diffDays}d ago"
        else -> "${diffDays / 7}w ago"
    }
}
