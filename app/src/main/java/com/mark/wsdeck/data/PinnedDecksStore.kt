package com.mark.wsdeck.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 使用者手動釘選到首頁的常用牌組，對應 iOS 的 PinnedDecksStore。不是依開牌
 * 次數自動排序——「順手」是使用者自己說了算，程式不用猜。存的是 Deck.uuid
 * 的字串，保留釘選順序（後釘選的排後面）。
 */
class PinnedDecksStore(context: Context) {
    private val prefs = Prefs(context)

    var uuids by mutableStateOf(prefs.pinnedDeckUuids)
        private set

    fun isPinned(uuid: String) = uuid in uuids

    fun toggle(uuid: String) {
        uuids = if (uuid in uuids) uuids - uuid else uuids + uuid
        prefs.pinnedDeckUuids = uuids
    }

    /** 牌組被刪除時一併清掉，不然首頁會留著指向不存在牌組的殘影 */
    fun remove(uuid: String) {
        if (uuid !in uuids) return
        uuids = uuids - uuid
        prefs.pinnedDeckUuids = uuids
    }
}
