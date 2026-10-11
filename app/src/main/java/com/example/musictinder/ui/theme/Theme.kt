package com.example.musictinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorWhite = Color(0xFFF2F2F7)
private val ColorBlack = Color(0xFF000000)
private val ColorTextSecondary = Color(0xFFB3B3B8)
private val ColorOutline = Color(0xFF5A5A60)

private val DarkColorScheme = darkColorScheme(
    primary = PinkPrimary,
    onPrimary = ColorWhite,
    primaryContainer = PinkContainer,
    onPrimaryContainer = OnPinkContainer,
    secondary = LikeGreen,
    onSecondary = ColorBlack,
    secondaryContainer = LikeGreenContainer,
    onSecondaryContainer = OnLikeGreenContainer,
    tertiary = StarYellow,
    onTertiary = ColorBlack,
    tertiaryContainer = StarYellowContainer,
    onTertiaryContainer = OnStarYellowContainer,
    error = RejectRed,
    background = BackgroundDark,
    onBackground = ColorWhite,
    surface = SurfaceDark,
    onSurface = ColorWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = ColorTextSecondary,
    outline = ColorOutline
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}