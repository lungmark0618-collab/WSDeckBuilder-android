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
import java.io.File

/**
 * 官網公告一則：新商品、卡表更新、大會、規則異動——見
 * WSDeckBuilder-data/tools/fetch_ws_news.py 產生的 ws_news.json。
 * 對應 iOS 的 WSNewsItem。
 */
@Serializable
data class WSNewsItem(
    val date: String,
    val categories: List<String> = emptyList(),
    @SerialName("title_jp") val titleJP: String,
    @SerialName("title_zh") val titleZH: String? = null,
    val url: String,
    val source: String = "official",
    /** 商品公告才有的規格重點（發售日、售價、卡片種類數），從官網商品頁的
     *  規格表抓來的事實資訊，不是公告全文的翻譯——見 WSDeckBuilder-data 的
     *  tools/enrich_ws_news.py。空清單代表這則公告沒有結構化規格可抓（規則
     *  更新、賽事公告等），詳情頁只會顯示標題跟官網連結 */
    @SerialName("highlights_zh") val highlightsZH: List<String> = emptyList(),
    /** 首頁輪播用的縮圖，不是每則都有——沒配圖的公告就不會出現在輪播裡 */
    @SerialName("image_url") val imageURL: String? = null,
) {
    /** 有中文說明就顯示中文，沒有就顯示官方日文原文——不擅自翻譯，只顯示有把握的內容 */
    val displayTitle: String get() = titleZH ?: titleJP
}

@Serializable
private data class WSNewsFeed(val items: List<WSNewsItem> = emptyList())

/**
 * 抓 WSDeckBuilder-data 發布的 ws_news.json，對應 iOS 的 WSNewsService，
 * 跟 DataUpdater／AnnouncementCenter 同一套「線上抓、本機快取、離線也能
 * 看上次結果」的作法。
 */
class WSNewsRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val cacheFile: File get() = File(context.cacheDir, "ws_news_cache.json")

    data class UiState(
        val items: List<WSNewsItem> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _ui = MutableStateFlow(UiState(items = loadCache()))
    val ui: StateFlow<UiState> = _ui

    companion object {
        private const val NEWS_URL =
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/ws_news.json"
    }

    private fun loadCache(): List<WSNewsItem> =
        try {
            if (cacheFile.exists()) cardJson.decodeFromString<WSNewsFeed>(cacheFile.readText()).items
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        if (_ui.value.isLoading) return@withContext
        _ui.update { it.copy(isLoading = true) }
        try {
            val request = Request.Builder()
                .url(NEWS_URL)
                .header("Cache-Control", "no-cache")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("伺服器回應 ${response.code}")
                response.body?.string() ?: throw java.io.IOException("沒有回應內容")
            }
            val feed = cardJson.decodeFromString<WSNewsFeed>(body)
            cacheFile.writeText(body)
            _ui.update { it.copy(items = feed.items, isLoading = false, errorMessage = null) }
        } catch (e: Exception) {
            // 抓不到就沿用快取，不拿錯誤訊息打斷使用者——首頁的公告不是關鍵功能
            _ui.update {
                it.copy(isLoading = false, errorMessage = "抓不到最新公告，顯示的是上次快取的內容。")
            }
        }
    }
}
