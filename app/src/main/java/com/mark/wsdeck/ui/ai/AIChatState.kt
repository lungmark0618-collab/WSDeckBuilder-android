package com.mark.wsdeck.ui.ai

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mark.wsdeck.data.AIAssistantService
import com.mark.wsdeck.data.AIAssistantServiceResolver
import com.mark.wsdeck.data.AICardContext
import com.mark.wsdeck.data.AIChatMessage
import com.mark.wsdeck.data.Card
import com.mark.wsdeck.data.RulesReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 浮動聊天視窗的狀態：整個 App 共用同一個對話（不分頁面），從卡片詳情頁
 * 點「問 AI」會把該卡的資料帶進來當背景資訊；問題送出後才知道要不要一併
 * 帶規則資料（規則問答跟卡牌問答用同一個輸入框，不用使用者自己切模式），
 * 對應 iOS 的 AIChatCoordinator
 */
class AIChatState(private val fixedService: AIAssistantService? = null) {
    var isPresented by mutableStateOf(false)
    val messages = mutableStateListOf<AIChatMessage>()
    var draftText by mutableStateOf("")
    var cardContext by mutableStateOf<AICardContext?>(null)
        private set

    /** 從卡片詳情頁的「問 AI」按鈕呼叫——換卡要提示一下，不然使用者以為
     *  AI 還記得上一張卡在問什麼 */
    fun open(card: Card) {
        if (cardContext?.card?.id != card.id) {
            cardContext = AICardContext(card)
            messages.add(AIChatMessage(
                role = AIChatMessage.Role.ASSISTANT,
                text = "已帶入「${card.nameZH}」的卡片資料，可以直接問這張卡的效果或規則問題。",
            ))
        }
        isPresented = true
    }

    /** 從浮動按鈕呼叫，一般規則問答，不特別綁定某張卡 */
    fun openGeneral() {
        isPresented = true
    }

    fun clearCardContext() {
        cardContext = null
    }

    fun send(context: Context, scope: CoroutineScope) {
        val question = draftText.trim()
        if (question.isEmpty()) return
        draftText = ""
        messages.add(AIChatMessage(role = AIChatMessage.Role.USER, text = question))
        val placeholder = AIChatMessage(role = AIChatMessage.Role.ASSISTANT, text = "", isLoading = true)
        messages.add(placeholder)
        val history = messages.dropLast(2)
        val card = cardContext
        val rules = RulesReference.text(context)
        val service = fixedService ?: AIAssistantServiceResolver.current(context)

        scope.launch {
            val reply = try {
                service.ask(question = question, history = history, cardContext = card, rulesContext = rules)
            } catch (e: Exception) {
                "問答失敗，請稍後再試一次。"
            }
            val index = messages.indexOfFirst { it.id == placeholder.id }
            if (index >= 0) {
                messages[index] = messages[index].copy(text = reply, isLoading = false)
            }
        }
    }
}
