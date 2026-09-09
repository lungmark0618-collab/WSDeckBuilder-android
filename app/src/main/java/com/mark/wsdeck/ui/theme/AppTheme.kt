package com.mark.wsdeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.mark.wsdeck.data.BackgroundStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.mark.wsdeck.data.AppearanceSettings
import com.mark.wsdeck.data.TextWeightOption

/**
 * 把 AppearanceSettings 套到整個 App，對應 iOS 的 View.appAppearance(_:)。
 * 跟 iOS 分工一樣：這裡管結構性外觀，個別畫面仍可覆蓋（例如卡圖上的固定白字）。
 */
@Composable
fun AppTheme(appearance: AppearanceSettings.UiState, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val finalScheme = remember(appearance, systemDark) { appearanceColorScheme(appearance, systemDark) }
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = finalScheme.background.luminance() > 0.179f
            controller.isAppearanceLightNavigationBars = finalScheme.background.luminance() > 0.179f
        }
    }

    val weight = fontWeightFor(appearance.textWeight)
    val typography = remember(weight) { weightedTypography(weight) }

    val density = LocalDensity.current
    val scaledDensity = remember(density, appearance.textSize) {
        Density(density = density.density, fontScale = density.fontScale * appearance.textSize.fontScale)
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(colorScheme = finalScheme, typography = typography) {
            Surface(Modifier.fillMaxSize(), color = finalScheme.background) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

/** 純配色運算，可驗證系統模式、自訂背景與文字對比。 */
internal fun appearanceColorScheme(appearance: AppearanceSettings.UiState, systemDark: Boolean): androidx.compose.material3.ColorScheme {
    val custom = Color(0xFF000000L or (appearance.customBackgroundHex.toLongOrNull(16) ?: 0xE8E4DCL))
    val isDark = if (appearance.background == BackgroundStyle.CUSTOM) custom.luminance() <= 0.179f
                 else appearance.background.forcesDark ?: systemDark
    val baseScheme = if (isDark) darkColorScheme() else lightColorScheme()
    val background = when (appearance.background) {
        BackgroundStyle.CUSTOM -> custom
        BackgroundStyle.PURE_BLACK -> Color.Black
        BackgroundStyle.LIGHT -> Color(0xFFF5F5F5)
        BackgroundStyle.DARK -> Color(0xFF0E0E0E)
        BackgroundStyle.SYSTEM -> if (isDark) Color(0xFF0E0E0E) else Color(0xFFF5F5F5)
        else -> appearance.background.color ?: baseScheme.background
    }
    val surfaceTarget = if (isDark && background.luminance() > 0.10f) Color.Black else Color.White
    val panel = lerp(background, surfaceTarget, if (isDark) 0.10f else 0.72f)
    val elevated = lerp(background, surfaceTarget, if (isDark) 0.16f else 0.90f)
    val accent = appearance.accentColor
    val colorScheme = baseScheme.copy(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.179f) Color.Black else Color.White,
        secondary = accent,
        tertiary = accent,
        background = background,
        surface = panel,
        surfaceVariant = elevated,
        surfaceContainer = panel,
        surfaceContainerLow = panel,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = elevated,
        surfaceContainerLowest = background,
        onBackground = if (isDark) Color.White else Color.Black,
        onSurface = if (isDark) Color.White else Color.Black,
    )

    val textColor = appearance.textTone.color(colorScheme)
    return if (textColor == null) colorScheme
        else colorScheme.copy(onBackground = textColor, onSurface = textColor)

}

private fun fontWeightFor(option: TextWeightOption): FontWeight? = when (option) {
    TextWeightOption.LIGHT -> FontWeight.Light
    TextWeightOption.REGULAR -> null // 留用各元件原本的字重，跟 iOS 一樣
    TextWeightOption.MEDIUM -> FontWeight.Medium
    TextWeightOption.BOLD -> FontWeight.SemiBold
}

/** 字重是 null 就直接用預設 Typography，不用另外複製一份浪費記憶體 */
private fun weightedTypography(weight: FontWeight?): Typography {
    val base = Typography()
    if (weight == null) return base
    return Typography(
        displayLarge = base.displayLarge.copy(fontWeight = weight),
        displayMedium = base.displayMedium.copy(fontWeight = weight),
        displaySmall = base.displaySmall.copy(fontWeight = weight),
        headlineLarge = base.headlineLarge.copy(fontWeight = weight),
        headlineMedium = base.headlineMedium.copy(fontWeight = weight),
        headlineSmall = base.headlineSmall.copy(fontWeight = weight),
        titleLarge = base.titleLarge.copy(fontWeight = weight),
        titleMedium = base.titleMedium.copy(fontWeight = weight),
        titleSmall = base.titleSmall.copy(fontWeight = weight),
        bodyLarge = base.bodyLarge.copy(fontWeight = weight),
        bodyMedium = base.bodyMedium.copy(fontWeight = weight),
        bodySmall = base.bodySmall.copy(fontWeight = weight),
        labelLarge = base.labelLarge.copy(fontWeight = weight),
        labelMedium = base.labelMedium.copy(fontWeight = weight),
        labelSmall = base.labelSmall.copy(fontWeight = weight),
    )
}
