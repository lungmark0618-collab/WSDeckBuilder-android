package com.mark.wsdeck.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * 首次使用的引導教學步驟，對應 iOS 的 OnboardingStep。順序即教學順序，每一步都
 * 對應畫面上一個真實的互動（不是截圖假操作），使用者做了那個動作，教學才會往下走。
 *
 * 步驟順序刻意先在「牌組」分頁建立一副牌組，再回「圖鑑」示範加卡——
 * 加卡動作需要有一副現有牌組才 make sense，順序反過來會卡住。
 */
enum class OnboardingStep(val title: String, val body: String, val tab: OnboardingTab?) {
    /** 純招呼語，不指任何元件、不算進「N / 總數」的步驟計數裡 */
    WELCOME(
        "歡迎使用",
        "歡迎使用本程式，接下來我會教你如何使用這些功能。",
        null,
    ),
    SEARCH(
        "搜尋卡片",
        "在上面的搜尋列輸入卡號、卡名或能力文字，試著打「hololive」看看。",
        OnboardingTab.CATALOG,
    ),
    FILTER(
        "篩選條件",
        "點篩選，可以用等級、顏色、種類縮小範圍。",
        OnboardingTab.CATALOG,
    ),
    NOTIFICATIONS(
        "通知",
        "開發者的公告和卡表更新，都會在這裡提醒你。",
        OnboardingTab.CATALOG,
    ),
    CREATE_DECK(
        "建立牌組",
        "點右下角的＋，建立你的第一副牌組。",
        OnboardingTab.DECKS,
    ),
    VIEW_CARD(
        "查看卡片",
        "回到圖鑑，點一部作品、再點一張卡，看看完整能力文字翻譯。",
        OnboardingTab.CATALOG,
    ),
    ADD_TO_DECK(
        "加入牌組",
        "在卡片上點「＋」，把它加進剛剛建立的牌組。",
        OnboardingTab.CATALOG,
    ),
    APPEARANCE(
        "外觀設定",
        "點「外觀」，字級、背景、強調色都能依你喜好調整。",
        OnboardingTab.SETTINGS,
    ),
    ;

    val index: Int get() = ordinal

    /** 提示卡右上角「N / 總數」用的編號，招呼語不算在內 */
    val displayIndex: Int? get() = if (this == WELCOME) null else ordinal

    companion object {
        val countedTotal: Int get() = entries.size - 1
    }
}

/** 教學步驟該切去哪個分頁，對應 MainActivity 裡的 Tab 路由 */
enum class OnboardingTab { CATALOG, DECKS, SETTINGS }

/**
 * 引導教學的狀態機，對應 iOS 的 OnboardingCoordinator。真正的「做了什麼」由各畫面
 * 自己在對應的動作裡呼叫 notify(_)，這裡只負責「目前該顯示第幾步、有沒有結束」。
 *
 * 用 mutableStateOf 而非 StateFlow——這個狀態只有 UI 讀，沒有背景協程要 collect，
 * 跟 iOS @Observable 的直接讀寫語意最接近，Compose 端 by state 就會自動重組。
 */
class OnboardingState(context: Context) {
    private val prefs = Prefs(context)

    var isActive by mutableStateOf(!prefs.onboardingCompleted)
        private set
    var currentStep by mutableStateOf(if (isActive) OnboardingStep.entries.first() else null)
        private set

    /**
     * 畫面回報「這一步的目標元件在畫面上的哪個位置」，疊層用這個畫出光圈，對應 iOS
     * anchorPreference 那一套。沒被回報的步驟（例如搜尋列，屬於系統輸入框不好量測）
     * 疊層就只顯示提示卡、不畫光圈——兩種都合理，不是漏做。
     */
    val anchors = mutableStateMapOf<OnboardingStep, Rect>()

    /** 各畫面在真正的動作發生時呼叫。只有「現在正好在等這一步」才會前進，
     *  使用者提早點到後面步驟的目標不會誤觸——那個元件這時候通常也還沒出現。 */
    fun notify(step: OnboardingStep) {
        if (!isActive || currentStep != step) return
        advance()
    }

    /** 教學卡片上的「下一步」，不管真實動作有沒有發生都放行——
     *  不能讓不知道要怎麼操作的人卡在某一步走不下去。 */
    fun advance() {
        val current = currentStep ?: return
        val next = current.index + 1
        if (next < OnboardingStep.entries.size) {
            currentStep = OnboardingStep.entries[next]
        } else {
            finish()
        }
    }

    /** 教學卡片上的「上一步」，回頭看漏掉或忘記的說明；
     *  已經在第一步就沒有更前面可以退了 */
    fun retreat() {
        val current = currentStep ?: return
        if (current.index == 0) return
        currentStep = OnboardingStep.entries[current.index - 1]
    }

    fun skip() = finish()

    /** 設定頁的「幫助」按鈕：讓看過教學的人也能重新從頭跑一次 */
    fun restart() {
        prefs.onboardingCompleted = false
        isActive = true
        currentStep = OnboardingStep.entries.first()
    }

    private fun finish() {
        isActive = false
        currentStep = null
        prefs.onboardingCompleted = true
    }
}
