package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode {
    SYSTEM,
    HIGH_CONTRAST_DARK,
    HIGH_CONTRAST_LIGHT,
    YELLOW_ON_BLACK
}

val StandardDarkScheme = darkColorScheme(
    primary = VibrantTeal,
    onPrimary = DarkNavy,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = PureWhite,
    secondary = WarningAmber,
    onSecondary = DarkNavy,
    background = DarkNavy,
    onBackground = PureWhite,
    surface = NavySurface,
    onSurface = PureWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = PureWhite,
    error = AlertRed,
    onError = PureWhite
)

val StandardLightScheme = lightColorScheme(
    primary = Color(0xFF005B96),
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF9C4146),
    onSecondary = PureWhite,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    error = AlertRed,
    onError = PureWhite
)

val HighContrastDarkScheme = darkColorScheme(
    primary = HighContrastDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003344),
    onPrimaryContainer = HighContrastDarkPrimary,
    secondary = HighContrastDarkSecondary,
    onSecondary = Color.Black,
    background = HighContrastDarkBg,
    onBackground = HighContrastDarkOn,
    surface = HighContrastDarkSurface,
    onSurface = HighContrastDarkOn,
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = HighContrastDarkOn,
    outline = HighContrastDarkPrimary,
    error = AlertRed,
    onError = Color.Black
)

val HighContrastLightScheme = lightColorScheme(
    primary = HighContrastLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE4FF),
    onPrimaryContainer = HighContrastLightPrimary,
    secondary = HighContrastLightSecondary,
    onSecondary = Color.White,
    background = HighContrastLightBg,
    onBackground = HighContrastLightOn,
    surface = HighContrastLightSurface,
    onSurface = HighContrastLightOn,
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = HighContrastLightOn,
    outline = Color.Black,
    error = AlertRed,
    onError = Color.White
)

val YellowOnBlackScheme = darkColorScheme(
    primary = YellowOnBlackPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333300),
    onPrimaryContainer = YellowOnBlackPrimary,
    secondary = YellowOnBlackSecondary,
    onSecondary = Color.Black,
    background = YellowOnBlackBg,
    onBackground = YellowOnBlackOn,
    surface = YellowOnBlackSurface,
    onSurface = YellowOnBlackOn,
    surfaceVariant = Color(0xFF2A2A00),
    onSurfaceVariant = YellowOnBlackOn,
    outline = YellowOnBlackPrimary,
    error = Color(0xFFFF4444),
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.HIGH_CONTRAST_DARK -> HighContrastDarkScheme
        AppThemeMode.HIGH_CONTRAST_LIGHT -> HighContrastLightScheme
        AppThemeMode.YELLOW_ON_BLACK -> YellowOnBlackScheme
        AppThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) StandardDarkScheme else StandardLightScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
