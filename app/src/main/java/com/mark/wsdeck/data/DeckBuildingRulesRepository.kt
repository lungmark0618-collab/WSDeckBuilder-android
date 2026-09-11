package com.mark.wsdeck.data

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * `deck_building_rules.json` 的解碼結構，只取用得到的欄位——`source_cards`
 * 那些附帶的原文摘錄只是例外表產生時的可追溯依據，App 端不需要（`cardJson`
 * 的 `ignoreUnknownKeys` 會自動略過）。
 */
@Serializable
private data class DeckBuildingRulesFeed(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("default_same_name_limit") val defaultSameNameLimit: Int,
    val rules: List<Rule> = emptyList(),
) {
    @Serializable
    data class Rule(
        @SerialName("names_jp") val namesJP: List<String> = emptyList(),
        val limit: Int? = null,
        @SerialName("limit_kind") val limitKind: String,
    )
}

/** Compose 環境值，對應 iOS 的 `@Environment(DeckBuildingRulesService.self)`，
 *  讓深層的卡片/牌組畫面不用一路把 repo 當參數往下傳 */
val LocalDeckBuildingRules = compositionLocalOf { NameLimitRules.standard }

private fun DeckBuildingRulesFeed.toRules(): NameLimitRules {
    val map = mutableMapOf<String, NameLimitRules.Group>()
    for (rule in rules) {
        val names = rule.namesJP.toSet()
        if (names.isEmpty()) continue
        val limit = if (rule.limitKind == "unlimited") null else rule.limit
        val group = NameLimitRules.Group(names = names, limit = limit)
        for (name in names) map[name] = group
    }
    return NameLimitRules(defaultLimit = defaultSameNameLimit, groupByName = map)
}

/**
 * 背景抓組牌限制例外表，對應 iOS 的 DeckBuildingRulesService，跟
 * WaveNameRepository 同一套「抓不到就沿用快取／預設值，不用錯誤打斷使用者」
 * 的作法——這只是少數卡片的規則細節，不是關鍵功能，查不到就照標準 4 張
 * 上限判斷，不會把原本合法的牌組誤判成違規。
 */
class DeckBuildingRulesRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val cacheFile: File get() = File(context.cacheDir, "deck_building_rules_cache.json")

    var rules: NameLimitRules by mutableStateOf(loadCache())
        private set

    companion object {
        private const val URL =
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/deck_building_rules.json"
        /** 舊格式一律不看，App 只認得看得懂的 schema，跟卡表更新同一套保護 */
        private const val SUPPORTED_SCHEMA_VERSION = 1
    }

    /** 非 Compose 情境（測試、純函式呼叫端）讀 DeckValidator.activeRules 就好，
     *  這裡同步更新一份，避免兩邊各自為政 */
    private fun apply(value: NameLimitRules) {
        rules = value
        DeckValidator.activeRules = value
    }

    init {
        DeckValidator.activeRules = rules
    }

    private fun loadCache(): NameLimitRules =
        try {
            if (cacheFile.exists()) {
                val feed = cardJson.decodeFromString<DeckBuildingRulesFeed>(cacheFile.readText())
                if (feed.schemaVersion <= SUPPORTED_SCHEMA_VERSION) feed.toRules() else NameLimitRules.standard
            } else {
                NameLimitRules.standard
            }
        } catch (e: Exception) {
            NameLimitRules.standard
        }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(URL)
                .header("Cache-Control", "no-cache")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("伺服器回應 ${response.code}")
                response.body?.string() ?: throw java.io.IOException("沒有回應內容")
            }
            val feed = cardJson.decodeFromString<DeckBuildingRulesFeed>(body)
            if (feed.schemaVersion > SUPPORTED_SCHEMA_VERSION) return@withContext
            apply(feed.toRules())
            cacheFile.writeText(body)
        } catch (e: Exception) {
            // 抓不到就沿用快取／標準規則，見上方類別註解
        }
    }
}
