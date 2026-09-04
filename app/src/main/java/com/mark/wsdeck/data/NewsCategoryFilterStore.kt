package com.mark.wsdeck.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 使用者選擇「首頁不想看到哪些分類」，對應 iOS 的 NewsCategoryFilterStore。
 * 存的是要隱藏的分類（不是要顯示的），這樣官網以後多出新分類時，預設還是
 * 顯示，不會因為沒被列進白名單就悄悄消失。
 */
class NewsCategoryFilterStore(context: Context) {
    private val prefs = Prefs(context)

    var hidden by mutableStateOf(prefs.hiddenNewsCategories)
        private set

    fun isHidden(category: String) = category in hidden

    fun toggle(category: String) {
        hidden = if (category in hidden) hidden - category else hidden + category
        prefs.hiddenNewsCategories = hidden
    }

    /** 一則公告只要還有任一分類沒被隱藏就顯示——公告常常同時掛好幾個分類，
     *  全部被使用者關掉了才真的濾掉 */
    fun isVisible(item: WSNewsItem): Boolean =
        item.categories.isEmpty() || !item.categories.all { it in hidden }
}
