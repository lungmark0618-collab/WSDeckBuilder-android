package com.mark.wsdeck.data

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * 卡圖批次預先下載＋快取管理，對應 iOS 的 ImageCache 裡設定頁用的那幾個函式。
 * 實際的載入／記憶體快取交給 Coil（WSDeckApp 裡設定的那個 ImageLoader）自己管，
 * 這裡只操作它已經有的 DiskCache，不重造一套快取機制。
 */
object ImageCacheOps {

    private fun loader(context: Context): ImageLoader = SingletonImageLoader.get(context)

    /**
     * Coil 預設用 request 的 data（這裡就是圖片網址字串）當磁碟快取 key。
     *
     * 公開這個函式是因為 `networkCachePolicy(DISABLED)` 實測攔不住 Coil 發網路請求——
     * 我們沒接 OkHttp 的 HTTP 快取，那個旗標管的是 HTTP 協定層的快取，不是「要不要連網路」。
     * 真正要擋，得先問過這裡，確定沒快取又不准連網路時，乾脆不建立 Coil request，
     * 直接顯示佔位圖（見 PolicyGatedCardImage）。
     */
    fun isCached(context: Context, url: String): Boolean {
        val cache = loader(context).diskCache ?: return false
        return cache.openSnapshot(url)?.also { it.close() } != null
    }

    /**
     * 併發上限 4，逐批回報進度；行動網路政策擋下時，剩下的直接回傳給呼叫端排進佇列，
     * 對應 iOS ImageCache.prefetch() 中途切到行動網路就暫停的邏輯。
     */
    suspend fun prefetch(
        context: Context,
        printings: List<Printing>,
        networkPolicy: NetworkPolicy,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<Printing> = withContext(Dispatchers.IO) {
        val missing = printings.filter { !isCached(context, it.imageURL) }
        val total = missing.size
        var done = 0
        var index = 0
        while (index < missing.size) {
            if (!networkPolicy.ui.value.allowsAutomaticDownload) {
                return@withContext missing.subList(index, missing.size)
            }
            val batch = missing.subList(index, minOf(index + 4, missing.size))
            index += batch.size
            batch.map { printing ->
                async { loader(context).execute(ImageRequest.Builder(context).data(printing.imageURL).build()) }
            }.awaitAll()
            done += batch.size
            onProgress(done, total)
        }
        emptyList()
    }

    fun cacheSizeBytes(context: Context): Long = loader(context).diskCache?.size ?: 0L

    /** 逐張問磁碟快取有沒有——一次性的設定頁動作，跟 iOS 掃目錄的開銷同量級 */
    suspend fun cachedCount(context: Context, printings: List<Printing>): Int =
        withContext(Dispatchers.IO) { printings.count { isCached(context, it.imageURL) } }

    fun clearCache(context: Context) {
        val cache = loader(context)
        cache.diskCache?.clear()
        cache.memoryCache?.clear()
    }
}
