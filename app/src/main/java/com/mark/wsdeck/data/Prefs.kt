package com.mark.wsdeck.data

import android.content.Context
import kotlinx.serialization.encodeToString

/**
 * 幾個跨畫面共用的小狀態，值只有一個字串或布林，不值得為此拉 DataStore。
 * 對應 iOS 的 @AppStorage("activeDeckUUID")。
 */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("wsdeck_prefs", Context.MODE_PRIVATE)

    var activeDeckUuid: String
        get() = prefs.getString(KEY_ACTIVE_DECK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_DECK, value).apply()

    /** 牌組卡表用圖片格子還是文字清單，對應 iOS 的 @AppStorage("deckUsesGrid") */
    var deckUsesGrid: Boolean
        get() = prefs.getBoolean(KEY_DECK_USES_GRID, true)
        set(value) = prefs.edit().putBoolean(KEY_DECK_USES_GRID, value).apply()

    /** 圖鑑搜尋結果用圖片格子還是文字清單，跟牌組卡表的設定分開記，
     *  對應 iOS CardCatalogView 自己的顯示模式開關 */
    var catalogUsesGrid: Boolean
        get() = prefs.getBoolean(KEY_CATALOG_USES_GRID, true)
        set(value) = prefs.edit().putBoolean(KEY_CATALOG_USES_GRID, value).apply()

    /** 上次靜默檢查卡表更新的時間（epoch ms），對應 iOS 的 lastCheckedAt */
    var cardDataLastCheckedAt: Long
        get() = prefs.getLong(KEY_CARD_DATA_CHECKED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_CARD_DATA_CHECKED_AT, value).apply()

    // MARK: - 通知（對應 iOS 的 AnnouncementCenter）

    /** 已讀通知的 id。SharedPreferences 原生支援字串集合，不用另外序列化 */
    var announcementReadIds: Set<String>
        get() = prefs.getStringSet(KEY_ANNOUNCEMENT_READ_IDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ANNOUNCEMENT_READ_IDS, value).apply()

    /** 使用者手動刪除過的通知 id，刪掉就永久從列表消失，對應 iOS 的 deletedIDs */
    var announcementDeletedIds: Set<String>
        get() = prefs.getStringSet(KEY_ANNOUNCEMENT_DELETED_IDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ANNOUNCEMENT_DELETED_IDS, value).apply()

    var notificationBadgeStyle: NotificationBadgeStyle
        get() = NotificationBadgeStyle.entries.firstOrNull {
            it.name == prefs.getString(KEY_NOTIFICATION_BADGE_STYLE, null)
        } ?: NotificationBadgeStyle.DOT
        set(value) = prefs.edit().putString(KEY_NOTIFICATION_BADGE_STYLE, value.name).apply()

    var announcementLastCheckedAt: Long
        get() = prefs.getLong(KEY_ANNOUNCEMENT_CHECKED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_ANNOUNCEMENT_CHECKED_AT, value).apply()

    /** 開場先用上次抓到的內容墊著，查更新是背景的事，鈴鐺不該先空著再彈出來 */
    var cachedAnnouncements: List<Announcement>
        get() {
            val json = prefs.getString(KEY_ANNOUNCEMENT_CACHE, null) ?: return emptyList()
            return try {
                cardJson.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) = prefs.edit()
            .putString(KEY_ANNOUNCEMENT_CACHE, cardJson.encodeToString(value)).apply()

    /** 本機自己合成的通知（卡表更新／新作品），對應 iOS 的 AnnouncementCenter.localItems */
    var cachedLocalAnnouncements: List<Announcement>
        get() {
            val json = prefs.getString(KEY_ANNOUNCEMENT_LOCAL_CACHE, null) ?: return emptyList()
            return try {
                cardJson.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) = prefs.edit()
            .putString(KEY_ANNOUNCEMENT_LOCAL_CACHE, cardJson.encodeToString(value)).apply()

    // MARK: - 外觀（對應 iOS 的 AppearanceSettings）

    var appTextSize: TextSize
        get() = enumOrDefault(KEY_APP_TEXT_SIZE, TextSize.STANDARD)
        set(value) = prefs.edit().putString(KEY_APP_TEXT_SIZE, value.name).apply()

    var appTextWeight: TextWeightOption
        get() = enumOrDefault(KEY_APP_TEXT_WEIGHT, TextWeightOption.REGULAR)
        set(value) = prefs.edit().putString(KEY_APP_TEXT_WEIGHT, value.name).apply()

    var appTextTone: TextTone
        get() = enumOrDefault(KEY_APP_TEXT_TONE, TextTone.STANDARD)
        set(value) = prefs.edit().putString(KEY_APP_TEXT_TONE, value.name).apply()

    var appBackground: BackgroundStyle
        get() = enumOrDefault(KEY_APP_BACKGROUND, BackgroundStyle.SYSTEM)
        set(value) = prefs.edit().putString(KEY_APP_BACKGROUND, value.name).apply()

    var appAccentMode: AccentMode
        get() = enumOrDefault(KEY_APP_ACCENT_MODE, AccentMode.FOLLOW_TITLE)
        set(value) = prefs.edit().putString(KEY_APP_ACCENT_MODE, value.name).apply()

    var appFixedAccent: AccentPreset
        get() = enumOrDefault(KEY_APP_FIXED_ACCENT, AccentPreset.ROSE)
        set(value) = prefs.edit().putString(KEY_APP_FIXED_ACCENT, value.name).apply()

    private inline fun <reified T : Enum<T>> enumOrDefault(key: String, default: T): T {
        val name = prefs.getString(key, null) ?: return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    // MARK: - 網路使用政策（對應 iOS 的 NetworkPolicy）

    var networkMode: NetworkPolicy.Mode
        get() = enumOrDefault(KEY_NETWORK_MODE, NetworkPolicy.Mode.WIFI_ONLY)
        set(value) = prefs.edit().putString(KEY_NETWORK_MODE, value.name).apply()

    var cellularBytesThisMonth: Long
        get() = prefs.getLong(KEY_CELLULAR_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_CELLULAR_BYTES, value).apply()

    var cellularStatsMonth: String
        get() = prefs.getString(KEY_CELLULAR_STATS_MONTH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CELLULAR_STATS_MONTH, value).apply()

    // MARK: - 新手教學（對應 iOS 的 OnboardingCoordinator）

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    // MARK: - 收藏作品（對應 iOS 的 FavoriteTitlesStore）

    var favoriteTitleCodes: Set<String>
        get() = prefs.getStringSet(KEY_FAVORITE_TITLE_CODES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAVORITE_TITLE_CODES, value).apply()

    // MARK: - 常用牌組（對應 iOS 的 PinnedDecksStore）
    // 用逗號分隔的字串而非 StringSet 存——釘選順序要保留，Set 不保證順序

    var pinnedDeckUuids: List<String>
        get() = prefs.getString(KEY_PINNED_DECK_UUIDS, "")
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_PINNED_DECK_UUIDS, value.joinToString(",")).apply()

    /** 卡片詳情頁要不要多顯示一份日文原文，對應 iOS AppearanceSettings.showJapanese */
    var showJapanese: Boolean
        get() = prefs.getBoolean(KEY_SHOW_JAPANESE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_JAPANESE, value).apply()

    // MARK: - 首頁公告分類篩選（對應 iOS 的 NewsCategoryFilterStore）
    /** 使用者關掉不想看的分類——存要隱藏的，不是要顯示的，官網以後多出新分類
     *  預設還是顯示，不會因為沒被列進白名單就悄悄消失 */
    var hiddenNewsCategories: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN_NEWS_CATEGORIES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_HIDDEN_NEWS_CATEGORIES, value).apply()

    companion object {
        private const val KEY_ACTIVE_DECK = "active_deck_uuid"
        private const val KEY_DECK_USES_GRID = "deck_uses_grid"
        private const val KEY_CATALOG_USES_GRID = "catalog_uses_grid"
        private const val KEY_CARD_DATA_CHECKED_AT = "card_data_last_checked_at"
        private const val KEY_ANNOUNCEMENT_READ_IDS = "announcement_read_ids"
        private const val KEY_ANNOUNCEMENT_DELETED_IDS = "announcement_deleted_ids"
        private const val KEY_NOTIFICATION_BADGE_STYLE = "notification_badge_style"
        private const val KEY_ANNOUNCEMENT_CHECKED_AT = "announcement_last_checked_at"
        private const val KEY_ANNOUNCEMENT_CACHE = "announcement_cache"
        private const val KEY_ANNOUNCEMENT_LOCAL_CACHE = "announcement_local_cache"
        private const val KEY_APP_TEXT_SIZE = "ap_text_size"
        private const val KEY_APP_TEXT_WEIGHT = "ap_text_weight"
        private const val KEY_APP_TEXT_TONE = "ap_text_tone"
        private const val KEY_APP_BACKGROUND = "ap_background"
        private const val KEY_APP_ACCENT_MODE = "ap_accent_mode"
        private const val KEY_APP_FIXED_ACCENT = "ap_fixed_accent"
        private const val KEY_NETWORK_MODE = "network_mode"
        private const val KEY_CELLULAR_BYTES = "cellular_bytes"
        private const val KEY_CELLULAR_STATS_MONTH = "cellular_stats_month"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_FAVORITE_TITLE_CODES = "favorite_title_codes"
        private const val KEY_PINNED_DECK_UUIDS = "pinned_deck_uuids"
        private const val KEY_HIDDEN_NEWS_CATEGORIES = "hidden_news_categories"
        private const val KEY_SHOW_JAPANESE = "show_japanese"
    }
}
