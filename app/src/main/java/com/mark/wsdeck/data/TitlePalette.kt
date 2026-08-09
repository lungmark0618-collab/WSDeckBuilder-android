package com.mark.wsdeck.data

import androidx.compose.ui.graphics.Color

/** 各作品的代表色，與 iOS 的 TitlePalette 對齊 */
object TitlePalette {
    fun accent(titleCode: String): Color = when (titleCode.uppercase()) {
        "BRD/W139" -> Color(0xFFB88038)   // 棕色塵埃：土黃
        "NIK" -> Color(0xFFD93347)        // 妮姬：紅
        "OVL" -> Color(0xFF7340A6)        // OVERLORD：暗紫
        "SFN" -> Color(0xFF4D9980)        // 芙莉蓮：青綠
        "BTR" -> Color(0xFFE67333)        // 孤獨搖滾：橘
        "CSM" -> Color(0xFFCC4D33)        // 鏈鋸人：鏽紅
        "HOL" -> Color(0xFF3399D9)        // hololive：天藍
        "UMA" -> Color(0xFF408C59)        // 賽馬娘：草綠
        "BD/W125" -> Color(0xFF5973BF)    // MyGO：靛藍
        "BD/W54" -> Color(0xFFF2739E)     // 少女樂團派對：粉紅
        "SPY" -> Color(0xFF4A5568)        // 間諜家家酒：石板灰
        "KGL" -> Color(0xFFD4548C)        // 輝夜姬：桃紅
        "TSK" -> Color(0xFF3FA8A0)        // 史萊姆：水藍綠
        "GIM" -> Color(0xFFE5B22E)        // 學園偶像大師：鮮黃
        "OSK" -> Color(0xFFB03FA8)        // 我推的孩子：星紫
        "PJS" -> Color(0xFF39C5BB)        // 世界計畫：初音青（官方色）
        "AZL" -> Color(0xFF1F4E8C)        // 碧藍航線：深海藍
        "LRC" -> Color(0xFFE8546B)        // 莉可麗絲：彼岸花紅
        else -> Color(0xFFD95999)
    }
}
