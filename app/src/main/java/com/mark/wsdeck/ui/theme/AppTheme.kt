package com.mark.wsdeck.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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
    // 玻璃感浮動分頁列（GlassTabBar）假設深色底，App 現在統一走固定深色調色盤，
    // 不再依 isSystemInDarkTheme() 或使用者的背景選項變化——對應 iOS 拿掉「背景」
    // 設定、AppSurface 全部寫死的決定
    val baseScheme = remember { darkColorScheme() }
    val accent = appearance.accentColor

    val colorScheme = remember(baseScheme, accent) {
        baseScheme.copy(
            primary = accent,
            secondary = accent,
            tertiary = accent,
            background = AppSurface.background,
            surface = AppSurface.panel,
        )
    }

    val textColor = appearance.textTone.color(colorScheme)
    val finalScheme = remember(colorScheme, textColor) {
        if (textColor == null) colorScheme
        else colorScheme.copy(onBackground = textColor, onSurface = textColor)
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
