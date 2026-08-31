package com.mark.wsdeck.data

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** 字級，對應 iOS 的 TextSize（Dynamic Type 沒有對應物，改用 fontScale 等比縮放） */
enum class TextSize(val label: String, val fontScale: Float) {
    COMPACT("最小", 0.85f),
    SMALL("小", 0.92f),
    STANDARD("標準", 1f),
    LARGE("大", 1.15f),
    HUGE("最大", 1.3f),
}

/** 字重，對應 iOS 的 TextWeight */
enum class TextWeightOption(val label: String) {
    LIGHT("細"),
    REGULAR("標準"),
    MEDIUM("中黑"),
    BOLD("粗"),
}

/** 文字色調，對應 iOS 的 TextTone */
enum class TextTone(val label: String) {
    STANDARD("系統"),
    WARM("暖白"),
    COOL("冷灰"),
    HIGH_CONTRAST("高對比");

    fun color(scheme: ColorScheme): Color? = when (this) {
        STANDARD -> null
        WARM -> Color(red = 0.36f, green = 0.30f, blue = 0.24f)
        COOL -> Color(red = 0.28f, green = 0.32f, blue = 0.38f)
        HIGH_CONTRAST -> scheme.onBackground
    }
}

/** 背景風格，對應 iOS 的 BackgroundStyle */
enum class BackgroundStyle(val label: String) {
    SYSTEM("跟隨系統"),
    LIGHT("淺色"),
    DARK("深色"),
    PURE_BLACK("純黑"),
    PAPER("米紙"),
    MIDNIGHT("深海藍");

    /** null＝跟隨系統，否則強制淺／深色 */
    val forcesDark: Boolean?
        get() = when (this) {
            SYSTEM -> null
            LIGHT, PAPER -> false
            DARK, PURE_BLACK, MIDNIGHT -> true
        }

    /** null＝用主題預設背景 */
    val color: Color?
        get() = when (this) {
            SYSTEM, LIGHT, DARK -> null
            PURE_BLACK -> Color.Black
            PAPER -> Color(red = 0.96f, green = 0.94f, blue = 0.88f)
            MIDNIGHT -> Color(red = 0.06f, green = 0.09f, blue = 0.16f)
        }
}

enum class AccentMode(val label: String) {
    FOLLOW_TITLE("跟著作品"),
    FIXED("固定一色"),
}

/** 固定強調色的選項，RGB 跟 iOS AccentPreset 一致 */
enum class AccentPreset(val label: String, val color: Color) {
    ROSE("玫瑰", Color(red = 0.85f, green = 0.35f, blue = 0.60f)),
    CRIMSON("緋紅", Color(red = 0.82f, green = 0.18f, blue = 0.25f)),
    AMBER("琥珀", Color(red = 0.90f, green = 0.60f, blue = 0.15f)),
    EMERALD("翡翠", Color(red = 0.15f, green = 0.65f, blue = 0.45f)),
    AZURE("天藍", Color(red = 0.15f, green = 0.50f, blue = 0.85f)),
    VIOLET("紫羅蘭", Color(red = 0.48f, green = 0.35f, blue = 0.80f)),
    GRAPHITE("石墨", Color(red = 0.40f, green = 0.42f, blue = 0.46f)),
}

/**
 * 外觀個人化設定，對應 iOS 的 AppearanceSettings：字體大小／粗細／文字色／背景色／強調色。
 * StateFlow 型態才跟專案裡其他資料層（DataUpdater、AnnouncementCenter）的模式一致。
 */
class AppearanceSettings(context: Context) {
    private val prefs = Prefs(context)

    data class UiState(
        val textSize: TextSize = TextSize.STANDARD,
        val textWeight: TextWeightOption = TextWeightOption.REGULAR,
        val textTone: TextTone = TextTone.STANDARD,
        val background: BackgroundStyle = BackgroundStyle.SYSTEM,
        val accentMode: AccentMode = AccentMode.FOLLOW_TITLE,
        val fixedAccent: AccentPreset = AccentPreset.ROSE,
        /** 目前瀏覽的作品（由圖鑑設定），供 accentMode == FOLLOW_TITLE 使用 */
        val currentTitleCode: String = "",
    ) {
        val accentColor: Color
            get() = when (accentMode) {
                AccentMode.FIXED -> fixedAccent.color
                AccentMode.FOLLOW_TITLE -> TitlePalette.accent(currentTitleCode)
            }
    }

    private val _ui = MutableStateFlow(
        UiState(
            textSize = prefs.appTextSize,
            textWeight = prefs.appTextWeight,
            textTone = prefs.appTextTone,
            background = prefs.appBackground,
            accentMode = prefs.appAccentMode,
            fixedAccent = prefs.appFixedAccent,
        ),
    )
    val ui: StateFlow<UiState> = _ui

    fun setTextSize(value: TextSize) {
        prefs.appTextSize = value
        _ui.update { it.copy(textSize = value) }
    }

    fun setTextWeight(value: TextWeightOption) {
        prefs.appTextWeight = value
        _ui.update { it.copy(textWeight = value) }
    }

    fun setTextTone(value: TextTone) {
        prefs.appTextTone = value
        _ui.update { it.copy(textTone = value) }
    }

    fun setBackground(value: BackgroundStyle) {
        prefs.appBackground = value
        _ui.update { it.copy(background = value) }
    }

    fun setAccentMode(value: AccentMode) {
        prefs.appAccentMode = value
        _ui.update { it.copy(accentMode = value) }
    }

    fun setFixedAccent(value: AccentPreset) {
        prefs.appFixedAccent = value
        _ui.update { it.copy(fixedAccent = value) }
    }

    /** 圖鑑畫面鎖定／離開某作品時呼叫，不落地存檔——跟 iOS 的 currentTitleCode 一樣是瞬時狀態 */
    fun setCurrentTitleCode(code: String) {
        if (_ui.value.currentTitleCode == code) return
        _ui.update { it.copy(currentTitleCode = code) }
    }
}
