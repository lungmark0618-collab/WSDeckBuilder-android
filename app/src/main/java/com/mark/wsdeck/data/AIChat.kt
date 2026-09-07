package com.mark.wsdeck.data

import android.content.Context
import java.util.UUID

/** 一句對話，使用者問的或 AI 答的（對應 iOS 的 AIChatMessage） */
data class AIChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    var text: String,
    var isLoading: Boolean = false,
) {
    enum class Role { USER, ASSISTANT }
}

/** 這次提問要帶給 AI 的背景資訊——從卡片詳情頁開啟時自動帶入這張卡的資料 */
data class AICardContext(val card: Card) {
    /** 組成餵給 AI 的卡片描述文字：中日文卡名、效果全文、數值、特徵 */
    val summary: String
        get() {
            val lines = mutableListOf<String>()
            lines += "卡名：${card.nameZH}（${card.nameJP}）"
            lines += "卡號：${card.id}"
            lines += "類型：${card.cardType.label}"
            card.color?.let { lines += "顏色：${it.label}" }
            card.level?.let { lines += "等級：$it" }
            card.cost?.let { lines += "花費：$it" }
            card.power?.let { lines += "力量：$it" }
            card.soul?.let { lines += "魂傷：$it" }
            card.trigger?.let { lines += "觸發符號：${it.label}" }
            if (card.traitsJP.isNotEmpty()) lines += "特徵：${card.traitsJP.joinToString("／")}"
            if (card.textZH.isNotEmpty()) lines += "效果文字（中文翻譯）：\n${card.textZH}"
            if (card.textJP.isNotEmpty()) lines += "效果文字（日文原文）：\n${card.textJP}"
            return lines.joinToString("\n")
        }
}

/**
 * AI 問答的後端接口——目前後端還沒定案（可能自架服務、也可能使用者自己貼
 * API Key），先用這個 interface 把「怎麼問」跟「怎麼答」隔開，UI／資料組裝
 * 邏輯可以先做，之後接真的服務只要換掉 MockAIAssistantService（對應 iOS
 * 的 AIAssistantService protocol）
 */
interface AIAssistantService {
    suspend fun ask(
        question: String,
        history: List<AIChatMessage>,
        cardContext: AICardContext?,
        rulesContext: String?,
    ): String
}

/** 開發階段先用假回覆讓聊天流程能先做出來，之後決定好後端接法再換掉這個實作 */
class MockAIAssistantService : AIAssistantService {
    override suspend fun ask(
        question: String,
        history: List<AIChatMessage>,
        cardContext: AICardContext?,
        rulesContext: String?,
    ): String {
        kotlinx.coroutines.delay(600)
        var reply = "（測試回覆，還沒接上真的 AI）你問的是：「$question」"
        cardContext?.card?.let { reply += "\n\n目前情境卡片：${it.nameZH}" }
        if (rulesContext != null) reply += "\n（已帶入規則資料）"
        return reply
    }
}

/**
 * 規則問答要餵給 AI 的背景資料——使用者之後會提供整理好的規則文件，屆時把
 * 檔案加進 app/src/main/assets/、檔名對上 FILE_NAME 即可，不用再改程式碼。
 * 找不到檔案時回傳 null，規則問答退化成只靠 AI 自己的知識回答（UI 上要
 * 提醒使用者這種情況答案不保證準確），對應 iOS 的 RulesReference。
 */
object RulesReference {
    private const val FILE_NAME = "WSRules.md"
    private var cached: String? = null
    private var loaded = false

    fun text(context: Context): String? {
        if (!loaded) {
            cached = try {
                context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
            } catch (e: java.io.IOException) {
                null
            }
            loaded = true
        }
        return cached
    }

    fun isAvailable(context: Context): Boolean = text(context) != null
}
