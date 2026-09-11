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
import android.os.SystemClock
import android.util.AtomicFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.TimeUnit

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
    /** 商品頁自己的大圖（包裝盒圖），比列表縮圖更清楚——只有商品類公告才有，
     *  詳情頁優先用這張，沒有的話退回用列表縮圖 */
    @SerialName("detail_image_url") val detailImageURL: String? = null,
) {
    /** 有中文說明就顯示中文，沒有就顯示官方日文原文——不擅自翻譯，只顯示有把握的內容 */
    val displayTitle: String get() = titleZH ?: titleJP
    /** 詳情頁該顯示的圖：優先用商品頁大圖，沒有就退回列表縮圖 */
    val bestImageURL: String? get() = detailImageURL ?: imageURL
}

@Serializable
private data class WSNewsFeed(val items: List<WSNewsItem> = emptyList())

/**
 * 抓 WSDeckBuilder-data 發布的 ws_news.json，對應 iOS 的 WSNewsService，
 * 跟 DataUpdater／AnnouncementCenter 同一套「線上抓、本機快取、離線也能
 * 看上次結果」的作法。
 */
class WSNewsRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build(),
) {
    private val refreshMutex = Mutex()
    private var lastAttemptAt: Long? = null
    private var lastSuccessAt = 0L
    private val cacheFile: File get() = File(context.cacheDir, "ws_news_cache.json")

    data class UiState(
        val items: List<WSNewsItem> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _ui = MutableStateFlow(UiState(items = loadCache()))
    val ui: StateFlow<UiState> = _ui

    init { if (_ui.value.items.isNotEmpty()) lastSuccessAt = cacheFile.lastModified() }

    companion object {
        private const val NEWS_URL =
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/ws_news.json"
    }

    private fun loadCache(): List<WSNewsItem> =
        try {
            if (cacheFile.exists()) cardJson.decodeFromString<WSNewsFeed>(AtomicFile(cacheFile).openRead().bufferedReader().use { it.readText() }).items
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    suspend fun refresh(force: Boolean = true) = withContext(Dispatchers.IO) {
        if (!refreshMutex.tryLock()) return@withContext
        try {
            if (!force) {
                val age = System.currentTimeMillis() - lastSuccessAt
                if (_ui.value.items.isNotEmpty() && age in 0 until 15 * 60_000L) return@withContext
                if (lastAttemptAt?.let { SystemClock.elapsedRealtime() - it < 60_000 } == true) return@withContext
            }
            lastAttemptAt = SystemClock.elapsedRealtime()
            _ui.update { it.copy(isLoading = true) }
            val request = Request.Builder()
                .url(NEWS_URL)
                .header("Cache-Control", "no-cache")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("伺服器回應 ${response.code}")
                response.body?.string() ?: throw java.io.IOException("沒有回應內容")
            }
            val feed = cardJson.decodeFromString<WSNewsFeed>(body)
            if (feed.items.isEmpty()) throw java.io.IOException("公告清單為空")
            // 磁碟暫時寫入失敗仍可顯示線上資料；原子寫入保留舊快取。
            val cache = AtomicFile(cacheFile)
            var stream: java.io.FileOutputStream? = null
            try {
                stream = cache.startWrite()
                stream.write(body.toByteArray(Charsets.UTF_8))
                cache.finishWrite(stream)
            } catch (_: java.io.IOException) { cache.failWrite(stream) }
            lastSuccessAt = System.currentTimeMillis()
            _ui.update { it.copy(items = feed.items, isLoading = false, errorMessage = null) }
        } catch (e: CancellationException) {
            lastAttemptAt = null
            throw e
        } catch (e: Exception) {
            // 抓不到就沿用快取，不拿錯誤訊息打斷使用者——首頁的公告不是關鍵功能
            _ui.update {
                it.copy(isLoading = false, errorMessage = "抓不到最新公告，顯示的是上次快取的內容。")
            }
        } finally {
            _ui.update { it.copy(isLoading = false) }
            refreshMutex.unlock()
        }
    }
}
