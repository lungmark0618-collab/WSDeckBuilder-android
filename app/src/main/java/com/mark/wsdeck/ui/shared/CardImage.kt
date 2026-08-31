package com.mark.wsdeck.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mark.wsdeck.data.ImageCacheOps
import com.mark.wsdeck.data.NetworkPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 卡圖顯示，受 NetworkPolicy 約束，對應 iOS 的 ImageCache.image(for:) / forceLoad(_:)。
 *
 * ⚠ 不是靠 Coil 的 networkCachePolicy 擋網路——那個旗標管的是 HTTP 協定層快取，
 * 我們沒接 OkHttp 的 response cache，實測設 DISABLED 攔不住真正的網路請求。
 * 真正的擋法：**先問磁碟快取有沒有，沒有又不准連網路，就乾脆不建立 Coil 的
 * request**，直接顯示佔位圖，Coil 完全不會被叫到、自然不會發網路請求。
 * 已快取的圖不受這層限制——那正是「看過一次的圖，之後永遠不用再下載」的來源。
 */
@Composable
fun PolicyGatedCardImage(
    url: String,
    contentDescription: String?,
    networkPolicy: NetworkPolicy,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val networkUi by networkPolicy.ui.collectAsStateWithLifecycle()
    // 使用者主動點過一次，這張圖之後就不再受政策限制
    var forceLoad by remember(url) { mutableStateOf(false) }
    val context = LocalContext.current

    // null＝還在問磁碟快取有沒有；問完之前不知道要不要顯示佔位圖，先當作「未知」
    var cached by remember(url) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(url) {
        cached = withContext(Dispatchers.IO) { ImageCacheOps.isCached(context, url) }
    }

    val allowLoad = cached == true || forceLoad || networkUi.allowsAutomaticDownload

    if (allowLoad) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        // 還沒問完磁碟快取，或問完確定沒快取且政策擋下——兩種狀況都先顯示同一種占位
        val tappable = cached != null && networkUi.allowsManualDownload
        Box(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .let { if (tappable) it.clickable { forceLoad = true } else it },
            contentAlignment = Alignment.Center,
        ) {
            if (cached != null) {
                Icon(
                    if (tappable) Icons.Filled.CloudDownload else Icons.Filled.CloudOff,
                    contentDescription = if (tappable) "點一下載入卡圖" else "卡圖未下載",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
