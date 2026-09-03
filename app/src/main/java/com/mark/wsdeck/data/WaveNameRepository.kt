package com.mark.wsdeck.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 拆彈作品每一彈的官方商品名稱標籤（如「Vol.2」「新装版」），對應
 * WSDeckBuilder-data/tools/make_wave_names.py 產生的 wave_names.json：
 * key 是 productCode（如 "SFN/S108"），value 是扣掉系列本名後剩下的部分，
 * 空字串代表「這彈就是官方原名，不用加標籤」。查不到的作品（該系列的官方
 * 名稱資料還不夠乾淨）就完全不會出現在這裡，CardRepository 端會自動退回
 * 舊的「第一彈/第二彈」數字猜測法。對應 iOS 的 WaveNameService。
 */
@Serializable
private data class WaveNameFeed(val waves: Map<String, String> = emptyMap())

class WaveNameRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val cacheFile: File get() = File(context.cacheDir, "wave_names_cache.json")

    var labels: Map<String, String> = loadCache()
        private set

    companion object {
        private const val URL =
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/wave_names.json"
    }

    private fun loadCache(): Map<String, String> =
        try {
            if (cacheFile.exists()) cardJson.decodeFromString<WaveNameFeed>(cacheFile.readText()).waves
            else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

    /** 回傳 true 代表拿到跟目前不一樣的新資料，呼叫端要重建圖鑑分類才看得到 */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(URL)
                .header("Cache-Control", "no-cache")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("伺服器回應 ${response.code}")
                response.body?.string() ?: throw java.io.IOException("沒有回應內容")
            }
            val feed = cardJson.decodeFromString<WaveNameFeed>(body)
            val changed = feed.waves != labels
            labels = feed.waves
            cacheFile.writeText(body)
            changed
        } catch (e: Exception) {
            // 抓不到就沿用快取／內建的數字猜測法，不用錯誤打斷使用者——
            // 這只是圖鑑分類標題的顯示細節，不是關鍵功能
            false
        }
    }
}
