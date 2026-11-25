package com.gymable.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun GymableTheme(
    accent: AccentOption = AccentOption.MINT_GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary                = accent.accent,
        onPrimary              = accent.onAccent,
        primaryContainer       = accent.accentContainer,
        onPrimaryContainer     = accent.onAccentContainer,
        secondary              = accent.accent,
        onSecondary            = accent.onAccent,
        secondaryContainer     = SurfaceHigh,
        onSecondaryContainer   = TextPrimary,
        tertiary               = RecordGold,
        onTertiary             = accent.onAccent,
        background             = Background,
        onBackground           = TextPrimary,
        surface                = Surface,
        onSurface              = TextPrimary,
        surfaceVariant         = SurfaceElevated,
        onSurfaceVariant       = TextSecondary,
        surfaceContainer       = SurfaceElevated,
        surfaceContainerHigh   = SurfaceHigh,
        surfaceContainerHighest = SurfaceHigh,
        surfaceContainerLow    = Surface,
        surfaceContainerLowest = Background,
        outline                = Outline,
        outlineVariant         = OutlineVariant,
        error                  = ErrorRed,
        onError                = OnError,
        scrim                  = Color(0xCC000000)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = Shapes,
        content     = content
    )
}
