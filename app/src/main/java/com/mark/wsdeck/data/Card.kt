package com.mark.wsdeck.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 卡表 JSON 的資料模型。欄位名必須與 iOS 版讀的是同一份檔案，
 * 所以這裡的 SerialName 是對外契約，不能為了 Kotlin 風格改動。
 *
 * 卡表由 tools/ 的 Python 管線產出，發佈在 WSDeckBuilder-data repo。
 */
@Serializable
data class CardSet(
    val meta: CardSetMeta,
    val cards: List<Card>,
)

@Serializable
data class CardSetMeta(
    @SerialName("title_code") val titleCode: String,
    @SerialName("title_name_jp") val titleNameJP: String,
    @SerialName("title_name_zh") val titleNameZH: String,
    @SerialName("card_count") val cardCount: Int,
    /** 單調遞增，線上更新就比這個數字。舊卡表沒有這個欄位，當作 1 */
    @SerialName("data_version") val dataVersion: Int = 1,
)

@Serializable
data class Card(
    val id: String,
    val printings: List<Printing>,
    @SerialName("name_jp") val nameJP: String,
    @SerialName("name_zh") val nameZH: String,
    @SerialName("card_type") val cardType: CardType,
    /** 極少數 SEC（隱藏）卡官方根本沒公開內容，連顏色都是 null，所以這裡是 nullable */
    val color: CardColor? = null,
    /** 事件與 CX 沒有等級 */
    val level: Int? = null,
    val cost: Int? = null,
    val power: Int? = null,
    val soul: Int? = null,
    val trigger: TriggerIcon? = null,
    /** 《》特徵刻意保留日文——對牌時要跟卡面長得一樣才好認 */
    @SerialName("traits_jp") val traitsJP: List<String> = emptyList(),
    @SerialName("text_jp") val textJP: String = "",
    @SerialName("text_zh") val textZH: String = "",
) {
    /** 普卡固定是第一個刷版 */
    val defaultPrinting: Printing get() = printings.first()

    /**
     * 商品代碼：卡號最後一個「-」前面的部分（如 "SFN/S108-024" → "SFN/S108"）。
     * 同系列常常橫跨好幾波不同商品，這是用來分開瀏覽用的依據——見
     * docs/series_breakdown_report.md（Codex 整理）："product code 取卡號
     * 最後一個 - 前面的部分"
     */
    val productCode: String get() = id.substringBeforeLast('-', id)

    /**
     * 搜尋比對用的預先串接字串。每次比對都重新組字串的話，
     * 4000 多張卡的即時搜尋會明顯頓——iOS 端也是同樣的處理。
     */
    val searchBlob: String by lazy {
        listOf(nameJP, nameZH, textJP, textZH).joinToString("\n").lowercase()
    }

    val textLinesZH: List<String> by lazy {
        if (textZH.isEmpty()) emptyList() else textZH.split("\n")
    }

    val textLinesJP: List<String> by lazy {
        if (textJP.isEmpty()) emptyList() else textJP.split("\n")
    }
}

@Serializable
data class Printing(
    val id: String,
    val rarity: String,
    @SerialName("image_url") val imageURL: String,
    @SerialName("is_foil") val isFoil: Boolean = false,
)

@Serializable
enum class CardType {
    @SerialName("character") CHARACTER,
    @SerialName("event") EVENT,
    @SerialName("climax") CLIMAX;

    val label: String
        get() = when (this) {
            CHARACTER -> "角色"
            EVENT -> "事件"
            CLIMAX -> "CX"
        }
}

@Serializable
enum class CardColor {
    @SerialName("yellow") YELLOW,
    @SerialName("green") GREEN,
    @SerialName("red") RED,
    @SerialName("blue") BLUE;

    val label: String
        get() = when (this) {
            YELLOW -> "黃"
            GREEN -> "綠"
            RED -> "紅"
            BLUE -> "藍"
        }
}

@Serializable
enum class TriggerIcon {
    @SerialName("soul") SOUL,
    @SerialName("soul2") SOUL2,
    @SerialName("gate") GATE,
    @SerialName("treasure") TREASURE,
    @SerialName("comeback") COMEBACK,
    @SerialName("draw") DRAW,
    @SerialName("pool") POOL,
    @SerialName("shot") SHOT,
    @SerialName("standby") STANDBY,
    @SerialName("choice") CHOICE;

    /** 依台灣圈子慣例（與 tools/glossary.json 一致） */
    val label: String
        get() = when (this) {
            SOUL -> "魂"
            SOUL2 -> "雙魂"
            GATE -> "城門"
            TREASURE -> "寶"
            COMEBACK -> "木門"
            DRAW -> "本"
            POOL -> "金"
            SHOT -> "槍"
            STANDBY -> "開機"
            CHOICE -> "箭頭"
        }
}

/** 卡表偶爾會多出這版 App 還不認得的欄位，忽略而不是整份解不開 */
val cardJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
