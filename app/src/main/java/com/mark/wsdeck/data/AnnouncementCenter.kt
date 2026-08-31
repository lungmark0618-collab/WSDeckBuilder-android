package com.mark.wsdeck.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 開發者想跟使用者說的一則話：這次更新了什麼、有什麼要分享的 */
@Serializable
data class Announcement(
    val id: String,
    val date: String,
    val title: String,
    val body: String,
)

@Serializable
private data class AnnouncementFeed(
    @SerialName("schema_version") val schemaVersion: Int,
    val items: List<Announcement> = emptyList(),
)

/** 鈴鐺右上角要顯示數字還是紅點，使用者自己在設定裡選 */
enum class NotificationBadgeStyle(val label: String) {
    DOT("紅點"),
    COUNT("數字"),
}

/**
 * 通知中心，對應 iOS 的 AnnouncementCenter。內容跟卡表走同一條線——同一個
 * 資料 repo 裡多一份 announcements.json，開發者要發通知就編輯那個檔案 push，
 * 不用重新上架 App。
 */
class AnnouncementCenter(context: Context, private val networkPolicy: NetworkPolicy) {

    data class UiState(
        val serverItems: List<Announcement> = emptyList(),
        /** 本機自己合成的通知（卡表更新／新作品），對應 iOS 的 localItems */
        val localItems: List<Announcement> = emptyList(),
        val readIds: Set<String> = emptySet(),
        /** 使用者手動刪除過的通知 id，刪掉就永久從列表消失，對應 iOS 的 deletedIDs */
        val deletedIds: Set<String> = emptySet(),
        val badgeStyle: NotificationBadgeStyle = NotificationBadgeStyle.DOT,
    ) {
        /** 兩邊合併、按日期排序、濾掉刪過的，畫面只認這個，不分來源 */
        val items: List<Announcement>
            get() = (serverItems + localItems)
                .filter { it.id !in deletedIds }
                .sortedByDescending { it.date }
        val unreadCount: Int get() = items.count { it.id !in readIds }
        fun isUnread(item: Announcement) = item.id !in readIds
    }

    private val prefs = Prefs(context)
    private val client = OkHttpClient()

    private val _ui = MutableStateFlow(
        UiState(
            serverItems = prefs.cachedAnnouncements,
            localItems = prefs.cachedLocalAnnouncements,
            readIds = prefs.announcementReadIds,
            deletedIds = prefs.announcementDeletedIds,
            badgeStyle = prefs.notificationBadgeStyle,
        ),
    )
    val ui: StateFlow<UiState> = _ui

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1

        /** 跟卡表 manifest 同一個 repo，理由一樣：raw 走 CDN、約快取 5 分鐘 */
        const val FEED_URL =
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/announcements.json"
        private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun setBadgeStyle(style: NotificationBadgeStyle) {
        prefs.notificationBadgeStyle = style
        _ui.update { it.copy(badgeStyle = style) }
    }

    fun markAllRead() {
        val ids = _ui.value.readIds + _ui.value.items.map { it.id }
        prefs.announcementReadIds = ids
        _ui.update { it.copy(readIds = ids) }
    }

    /** 看完想清掉就刪，不用留著——刪除是永久的，之後同一則（同 id）不會再出現 */
    fun delete(item: Announcement) {
        val ids = _ui.value.deletedIds + item.id
        prefs.announcementDeletedIds = ids
        _ui.update { it.copy(deletedIds = ids) }
    }

    /** 左上角「全部刪除」，一次清光目前看得到的所有通知 */
    fun deleteAll() {
        val ids = _ui.value.deletedIds + _ui.value.items.map { it.id }
        prefs.announcementDeletedIds = ids
        _ui.update { it.copy(deletedIds = ids) }
    }

    /** App 啟動時呼叫，一天查一次，查不到就沿用快取（跟 DataUpdater.checkSilently 同款） */
    suspend fun checkSilently() {
        val last = prefs.announcementLastCheckedAt
        if (last > 0 && System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return
        if (!networkPolicy.ui.value.allowsAutomaticDownload) return
        check()
    }

    suspend fun check() {
        try {
            val bytes = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(FEED_URL)
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("伺服器回應 ${response.code}")
                    response.body?.bytes() ?: throw IOException("沒有回應內容")
                }
            }
            val feed = cardJson.decodeFromString<AnnouncementFeed>(String(bytes))
            if (feed.schemaVersion > SUPPORTED_SCHEMA_VERSION) return
            val sorted = feed.items.sortedByDescending { it.date }
            prefs.cachedAnnouncements = sorted
            prefs.announcementLastCheckedAt = System.currentTimeMillis()
            _ui.update { it.copy(serverItems = sorted) }
        } catch (e: Exception) {
            // 靜默失敗，沿用快取內容——通知不值得為了查不到而跳錯誤打擾使用者
        }
    }

    /**
     * 查完卡表更新後呼叫，把「有更新可裝」轉成通知列表裡的一則，對應 iOS 同名函式。
     * 同一部作品同一個版本只會生一次通知——id 帶版本號，版本沒變就不重複，
     * 版本真的往上跳了才會是新的一則、重新變成未讀。
     */
    fun noteDataUpdates(pending: List<DataUpdater.Pending>) {
        val existing = _ui.value.localItems
        val existingIds = existing.map { it.id }.toSet()
        val today = dateFormat.format(Date())
        val added = pending.mapNotNull { item ->
            val id = "data-update-${item.titleCode}-${item.toVersion}"
            if (id in existingIds) return@mapNotNull null
            val isNewTitle = item.fromVersion == 0
            Announcement(
                id = id,
                date = today,
                title = if (isNewTitle) "新增了「${item.titleName}」" else "「${item.titleName}」卡表已更新",
                body = if (isNewTitle) {
                    "可以在圖鑑分頁看到這部新收錄的作品。"
                } else {
                    "有新的翻譯或卡片內容，到設定頁按「檢查更新」即可下載。"
                },
            )
        }
        if (added.isEmpty()) return
        val updated = (existing + added).sortedByDescending { it.date }
        prefs.cachedLocalAnnouncements = updated
        _ui.update { it.copy(localItems = updated) }
    }
}
