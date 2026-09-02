package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppBackgroundStyle
import com.example.data.model.AppThemeMode

// 1. Sultan Gold (Signature Dark)
private val SultanDarkColorScheme = darkColorScheme(
    primary = SultanGold,
    onPrimary = ObsidianBlack,
    primaryContainer = SultanGoldDark,
    onPrimaryContainer = SultanGoldLight,
    secondary = SultanAmber,
    onSecondary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

// 2. Midnight AMOLED (Pure Pitch Black)
private val MidnightAmoledColorScheme = darkColorScheme(
    primary = Color(0xFFFACC15),
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF854D0E),
    onPrimaryContainer = Color(0xFFFEF08A),
    secondary = Color(0xFF38BDF8),
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = DarkTextPrimary,
    surface = PureBlackSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = PureBlackSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = PureBlackBorder
)

// 3. Cyberpunk Neon & Violet
private val CyberNeonColorScheme = darkColorScheme(
    primary = CyberNeonCyan,
    onPrimary = CyberBg,
    primaryContainer = Color(0xFF005662),
    onPrimaryContainer = Color(0xFFB2F5EA),
    secondary = CyberNeonPurple,
    onSecondary = CyberBg,
    background = CyberBg,
    onBackground = DarkTextPrimary,
    surface = CyberSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = CyberBorder
)

// 4. Emerald Royale
private val EmeraldRoyaleColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldBg,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = EmeraldAccent,
    onSecondary = EmeraldBg,
    background = EmeraldBg,
    onBackground = DarkTextPrimary,
    surface = EmeraldSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = EmeraldBorder
)

// 5. Sunset Crimson
private val SunsetCrimsonColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetBg,
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = SunsetAccent,
    onSecondary = SunsetBg,
    background = SunsetBg,
    onBackground = DarkTextPrimary,
    surface = SunsetSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = SunsetBorder
)

// 6. Nordic Aurora
private val NordicAuroraColorScheme = darkColorScheme(
    primary = AuroraPrimary,
    onPrimary = AuroraBg,
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = AuroraAccent,
    onSecondary = AuroraBg,
    background = AuroraBg,
    onBackground = DarkTextPrimary,
    surface = AuroraSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = AuroraBorder
)

// 7. Frosted Pearl (Light Theme)
private val FrostedPearlColorScheme = lightColorScheme(
    primary = PearlPrimary,
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = PearlAccent,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

data class SultanThemeConfig(
    val themeMode: AppThemeMode = AppThemeMode.SULTAN_GOLD,
    val backgroundStyle: AppBackgroundStyle = AppBackgroundStyle.AMBIENT_GLOW,
    val backgroundBrush: Brush = Brush.linearGradient(listOf(ObsidianBlack, ObsidianBlack))
)

val LocalSultanThemeConfig = staticCompositionLocalOf { SultanThemeConfig() }

@Composable
fun SultanGalleryTheme(
    themeMode: AppThemeMode = AppThemeMode.SULTAN_GOLD,
    backgroundStyle: AppBackgroundStyle = AppBackgroundStyle.AMBIENT_GLOW,
    darkTheme: Boolean = true,
    isAmoled: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val activeMode = if (isAmoled) AppThemeMode.MIDNIGHT_AMOLED else themeMode

    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        activeMode == AppThemeMode.MIDNIGHT_AMOLED -> MidnightAmoledColorScheme
        activeMode == AppThemeMode.CYBER_NEON -> CyberNeonColorScheme
        activeMode == AppThemeMode.EMERALD_ROYALE -> EmeraldRoyaleColorScheme
        activeMode == AppThemeMode.SUNSET_AMBER -> SunsetCrimsonColorScheme
        activeMode == AppThemeMode.NORDIC_AURORA -> NordicAuroraColorScheme
        activeMode == AppThemeMode.FROSTED_PEARL -> FrostedPearlColorScheme
        else -> if (darkTheme) SultanDarkColorScheme else FrostedPearlColorScheme
    }

    val backgroundBrush = when (backgroundStyle) {
        AppBackgroundStyle.SOLID -> {
            Brush.linearGradient(listOf(colorScheme.background, colorScheme.background))
        }
        AppBackgroundStyle.AMBIENT_GLOW -> {
            Brush.radialGradient(
                colors = listOf(
                    colorScheme.primary.copy(alpha = 0.12f),
                    colorScheme.surface.copy(alpha = 0.5f),
                    colorScheme.background
                ),
                center = Offset(300f, 150f),
                radius = 900f
            )
        }
        AppBackgroundStyle.MESH_GRADIENT -> {
            Brush.linearGradient(
                colors = listOf(
                    colorScheme.surfaceVariant,
                    colorScheme.background,
                    colorScheme.primary.copy(alpha = 0.08f),
                    colorScheme.background
                ),
                start = Offset.Zero,
                end = Offset(1000f, 1800f)
            )
        }
        AppBackgroundStyle.GLASS_ACCENT -> {
            Brush.verticalGradient(
                colors = listOf(
                    colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    colorScheme.background
                )
            )
        }
    }

    val themeConfig = SultanThemeConfig(
        themeMode = activeMode,
        backgroundStyle = backgroundStyle,
        backgroundBrush = backgroundBrush
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                val isLight = activeMode == AppThemeMode.FROSTED_PEARL || !darkTheme
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
        }
    }

    CompositionLocalProvider(LocalSultanThemeConfig provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

