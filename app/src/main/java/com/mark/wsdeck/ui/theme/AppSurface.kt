package com.mark.wsdeck.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 固定的深色玻璃感面板色票，對應 iOS DesignTokens.swift 的 AppSurface enum。
 * App 現在統一走這套固定深色調色盤，不再讓使用者自訂背景主題（原本外觀設定裡
 * 的「背景」選項已拿掉——玻璃感浮動分頁列本來就假設深色底，讓使用者選淺色
 * 只會讓分頁列跟內容顏色打架）。
 */
object AppSurface {
    val background = Color(red = 0.02f, green = 0.02f, blue = 0.025f)
    val panel = Color(red = 0.11f, green = 0.11f, blue = 0.12f)
    val panelElevated = Color(red = 0.15f, green = 0.15f, blue = 0.17f)
    val hairline = Color.White.copy(alpha = 0.10f)
    val secondaryText = Color(red = 0.66f, green = 0.63f, blue = 0.70f)
}
