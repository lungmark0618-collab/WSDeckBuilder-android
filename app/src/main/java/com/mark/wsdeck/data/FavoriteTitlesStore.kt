package com.mark.wsdeck.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 使用者收藏／持續關注的作品，對應圖鑑作品選單卡片右上角的星星，對應 iOS 的
 * FavoriteTitlesStore。純粹是一組 titleCode，跟卡表資料本身無關——收藏的是
 * 「我對這部作品有興趣」，不是卡片持有狀態（那是 CollectionRepository 的事）。
 */
class FavoriteTitlesStore(context: Context) {
    private val prefs = Prefs(context)

    var titleCodes by mutableStateOf(prefs.favoriteTitleCodes)
        private set

    fun isFavorite(titleCode: String) = titleCode in titleCodes

    fun toggle(titleCode: String) {
        titleCodes = if (titleCode in titleCodes) titleCodes - titleCode else titleCodes + titleCode
        prefs.favoriteTitleCodes = titleCodes
    }

    /** 系列拆彈後，把「收藏了整個系列」展開成「底下每個商品都收藏」，
     *  不然舊資料格式（純 titleCode）在拆彈後會對不到任何 BrowsableSet.id，
     *  使用者原本收藏的東西就憑空消失了。對應 iOS 的
     *  FavoriteTitlesStore.migrate(using:)。
     *
     *  用商品代碼本身當有沒有跑過遷移的依據：遷移完 titleCodes 裡不會再留
     *  下已經拆彈的裸 titleCode，天然冪等，不用另外開關記錄跑過沒。 */
    fun migrate(repo: CardRepository) {
        var updated = titleCodes
        var changed = false
        for (code in titleCodes) {
            val matches = repo.snapshot.browsableSets.filter { it.titleCode == code }
            if (matches.size <= 1) continue
            updated = updated - code + matches.map { it.id }.toSet()
            changed = true
        }
        if (!changed) return
        titleCodes = updated
        prefs.favoriteTitleCodes = titleCodes
    }
}
