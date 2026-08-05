package com.mark.wsdeck

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

private const val BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

class WSDeckApp : Application(), SingletonImageLoader.Factory {

    /**
     * ws-tcg.com 的卡圖伺服器**沒有瀏覽器 User-Agent 就回 404**。
     * 這是實測出來的（無 UA → 404、有 UA → 200），iOS 端的 ImageCache
     * 也同樣要設，抓取腳本 fetch_cards.py 亦然。
     */
    private val http by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", BROWSER_UA)
                        .build()
                )
            }
            .build()
    }

    /**
     * Coil 3 不會自動註冊網路載入器——少了這一步卡圖就是一片空白，
     * 而且不會有任何錯誤訊息，很難查。
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { http })) }
            .memoryCache {
                // 記憶體上限壓在可用記憶體的 25%。卡圖網格很容易吃爆記憶體
                // 被系統終止，iOS 端也是同樣的考量
                MemoryCache.Builder().maxSizePercent(context, 0.25).build()
            }
            .diskCache {
                // 刻意不用 cacheDir——系統空間不足時會把它清掉。
                // 目標是「第一次看過的卡圖之後永遠不用再下載」，
                // 所以放 filesDir 底下自己管。
                DiskCache.Builder()
                    .directory((this as Context).filesDir.resolve("card_images").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
