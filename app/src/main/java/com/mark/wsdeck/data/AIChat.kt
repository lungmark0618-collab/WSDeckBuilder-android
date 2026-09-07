package com.mark.wsdeck.data

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

/** 使用者還沒到「設定」頁填代理伺服器網址時的替身，直接回一個引導訊息，
 *  不會真的發網路請求 */
class UnconfiguredAIAssistantService : AIAssistantService {
    override suspend fun ask(
        question: String,
        history: List<AIChatMessage>,
        cardContext: AICardContext?,
        rulesContext: String?,
    ): String = "尚未設定 AI 服務。請到「設定 → AI 服務設定」填入代理伺服器的網址與密鑰後再試一次。"
}

@Serializable
private data class AskRequestTurn(val role: String, val text: String)

@Serializable
private data class AskRequestBody(
    val question: String,
    val history: List<AskRequestTurn>,
    val cardContext: String? = null,
    val rulesContext: String? = null,
)

@Serializable
private data class AskResponseBody(val answer: String? = null, val error: String? = null)

/** 呼叫自架的輕量代理伺服器（見 ai-proxy/），伺服器再轉打 OpenAI，
 *  App 本身不帶 OpenAI Key，只帶一組共用密鑰擋住隨便打進來的請求 */
class RemoteAIAssistantService(
    private val baseUrl: String,
    private val sharedSecret: String,
) : AIAssistantService {
    private val client = OkHttpClient()

    override suspend fun ask(
        question: String,
        history: List<AIChatMessage>,
        cardContext: AICardContext?,
        rulesContext: String?,
    ): String = withContext(Dispatchers.IO) {
        val turns = history.map {
            AskRequestTurn(if (it.role == AIChatMessage.Role.ASSISTANT) "assistant" else "user", it.text)
        }
        val payload = AskRequestBody(question, turns, cardContext?.summary, rulesContext)
        val body = cardJson.encodeToString(payload).toRequestBody("application/json".toMediaType())
        val url = baseUrl.trimEnd('/') + "/ask"
        val requestBuilder = Request.Builder().url(url).post(body)
        if (sharedSecret.isNotEmpty()) {
            requestBuilder.addHeader("X-App-Secret", sharedSecret)
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw java.io.IOException("AI 代理伺服器錯誤（${response.code}）：$text")
            }
            val decoded = cardJson.decodeFromString<AskResponseBody>(text)
            if (decoded.error != null) throw java.io.IOException(decoded.error)
            decoded.answer.orEmpty()
        }
    }
}

/** 依「設定」頁目前存的代理伺服器網址／密鑰，決定要用真的服務還是引導訊息，
 *  每次問答都重新讀一次，設定改了不用重開 App */
object AIAssistantServiceResolver {
    fun current(context: Context): AIAssistantService {
        val prefs = Prefs(context)
        val url = prefs.aiProxyUrl.trim()
        if (url.isEmpty()) return UnconfiguredAIAssistantService()
        return RemoteAIAssistantService(url, prefs.aiProxySharedSecret)
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
