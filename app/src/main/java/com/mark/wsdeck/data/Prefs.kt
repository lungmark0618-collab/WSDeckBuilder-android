package com.mark.wsdeck.data

import android.content.Context

/**
 * 幾個跨畫面共用的小狀態，值只有一個字串或布林，不值得為此拉 DataStore。
 * 對應 iOS 的 @AppStorage("activeDeckUUID")。
 */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("wsdeck_prefs", Context.MODE_PRIVATE)

    var activeDeckUuid: String
        get() = prefs.getString(KEY_ACTIVE_DECK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_DECK, value).apply()

    companion object {
        private const val KEY_ACTIVE_DECK = "active_deck_uuid"
    }
}
