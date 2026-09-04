package com.mark.wsdeck.data

import androidx.compose.ui.graphics.Color

/**
 * 官網公告分類——原始值是日文（跟官網 HTML 一致，篩選記錄跟著用這個當 key
 * 才穩定，不會因為顯示文字改版而失效），顯示一律轉繁中。對應 iOS 的 NewsCategory。
 */
object NewsCategory {
    /** 官網公告目前會出現的所有分類，順序即篩選畫面顯示順序 */
    val all = listOf("商品情報", "カードリスト", "ルール", "デッキレシピ", "大会", "イベント", "お知らせ")

    fun labelZH(category: String): String = when (category) {
        "商品情報" -> "商品資訊"
        "カードリスト" -> "卡表"
        "ルール" -> "規則"
        "デッキレシピ" -> "牌組配方"
        "大会" -> "大會"
        "イベント" -> "活動"
        "お知らせ" -> "公告"
        else -> category
    }

    /** 卡角燙金色塊、寶石標記共用的飽和色 */
    fun color(category: String): Color = when (category) {
        "商品情報" -> Color(0xFF2196F3)
        "カードリスト" -> Color(0xFF4CAF50)
        "大会", "イベント" -> Color(0xFFFF9800)
        "ルール" -> Color(0xFF9C27B0)
        "デッキレシピ" -> Color(0xFFE91E63)
        else -> Color(0xFF9E9E9E)
    }

    /** 分類文字用的淺色調，飽和色直接當文字色在深底上太刺眼 */
    fun tint(category: String): Color = when (category) {
        "商品情報" -> Color(0xFF6DB8FF)
        "カードリスト" -> Color(0xFF7BDF9E)
        "大会", "イベント" -> Color(0xFFFFBD6B)
        "ルール" -> Color(0xFFD9A3FF)
        "デッキレシピ" -> Color(0xFFFF8FAB)
        else -> Color(0xFF9E9E9E)
    }
}
