package com.mark.wsdeck.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Palette
import com.mark.wsdeck.data.AnnouncementCenter
import com.mark.wsdeck.data.AppUpdater
import com.mark.wsdeck.data.AppearanceSettings
import com.mark.wsdeck.data.CardDataStore
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.DataUpdater
import com.mark.wsdeck.data.ImageCacheOps
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.NotificationBadgeStyle
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingStep
import com.mark.wsdeck.data.Printing
import com.mark.wsdeck.ui.onboarding.onboardingAnchor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 設定：卡表線上更新，對應 iOS SettingsView 的「卡表」區塊。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    cardRepo: CardRepository,
    updater: DataUpdater,
    appUpdater: AppUpdater,
    announcements: AnnouncementCenter,
    appearance: AppearanceSettings,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
    onOpenAppearance: () -> Unit,
) {
    val ui by updater.ui.collectAsStateWithLifecycle()
    val appUpdateState by appUpdater.state.collectAsStateWithLifecycle()
    val announcementUi by announcements.ui.collectAsStateWithLifecycle()
    val networkUi by networkPolicy.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var cacheSize by remember { mutableStateOf(0L) }
    var cachedCount by remember { mutableStateOf(0) }
    var prefetchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var confirmPrefetch by remember { mutableStateOf<PrefetchScope?>(null) }
    var confirmClearCache by remember { mutableStateOf(false) }

    val allPrintings = remember(cardRepo.snapshot.cards) {
        cardRepo.snapshot.cards.flatMap { it.printings }
    }
    fun printingsFor(target: PrefetchScope): List<Printing> = when (target) {
        PrefetchScope.NORMAL_ONLY -> cardRepo.snapshot.cards.map { it.defaultPrinting }
        PrefetchScope.ALL_PRINTINGS -> allPrintings
    }

    suspend fun refreshCacheInfo() {
        cacheSize = ImageCacheOps.cacheSizeBytes(context)
        cachedCount = ImageCacheOps.cachedCount(context, allPrintings)
    }
    LaunchedEffect(Unit) { refreshCacheInfo() }

    fun startPrefetch(target: PrefetchScope) {
        val targets = printingsFor(target)
        prefetchProgress = 0 to targets.size
        scope.launch {
            val remaining = ImageCacheOps.prefetch(context, targets, networkPolicy) { done, total ->
                prefetchProgress = done to total
            }
            // 下載到一半斷去 Wi-Fi 才會有剩的——排進佇列，等回到 Wi-Fi 自動接著載
            if (remaining.isNotEmpty()) networkPolicy.queuePrefetchForWiFi(remaining)
            prefetchProgress = null
            refreshCacheInfo()
        }
    }

    fun requestPrefetch(target: PrefetchScope) {
        // 不論政策設定為何，行動網路下批次預載都要確認
        if (networkUi.prefetchNeedsConfirmation) confirmPrefetch = target else startPrefetch(target)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("設定") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = onOpenAppearance,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.onboardingAnchor(OnboardingStep.APPEARANCE, onboarding),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("外觀", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            Text(
                "字體大小與粗細、文字與背景顏色、強調色。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text("卡表資料", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("收錄", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${cardRepo.snapshot.sets.size} 部作品 · ${cardRepo.snapshot.cards.size} 張",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (val s = ui.state) {
                is DataUpdater.State.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("檢查中…", style = MaterialTheme.typography.bodyMedium)
                }

                is DataUpdater.State.Downloading -> Column {
                    Text("更新中… ${s.done}/${s.total}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (s.total == 0) 0f else s.done.toFloat() / s.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is DataUpdater.State.UpdateAvailable -> Button(
                    onClick = { scope.launch { updater.performUpdate(s.pending, cardRepo) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("更新 ${s.pending.size} 部作品的卡表") }

                else -> Button(
                    onClick = {
                        scope.launch {
                            updater.check(cardRepo, silent = false)
                            (updater.ui.value.state as? DataUpdater.State.UpdateAvailable)?.let {
                                announcements.noteDataUpdates(it.pending)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("檢查更新") }
            }

            when (val s = ui.state) {
                is DataUpdater.State.UpToDate -> Text(
                    "已是最新版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                )
                is DataUpdater.State.Failed -> Text(
                    s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> {}
            }

            ui.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ui.lastCheckedAt?.let {
                Text(
                    "上次檢查：${formatTimestamp(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Text("通知", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NotificationBadgeStyle.entries.forEach { style ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        RadioButton(
                            selected = announcementUi.badgeStyle == style,
                            onClick = { announcements.setBadgeStyle(style) },
                        )
                        Text(style.label)
                    }
                }
            }
            Text(
                "有新通知時，圖鑑分頁右上角的鈴鐺顯示紅點或未讀則數。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text("網路使用政策", style = MaterialTheme.typography.titleMedium)
            Column {
                NetworkPolicy.Mode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = networkUi.mode == mode,
                            onClick = { networkPolicy.setMode(mode) },
                        )
                        Text(mode.label)
                    }
                }
            }
            Text(
                networkUi.mode.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (networkUi.isConstrained) {
                Text(
                    "目前系統為資料節省模式，自動下載已暫停",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE68A00),
                )
            }
            if (networkUi.cellularBytesThisMonth > 0) {
                Text(
                    "本月經由行動網路下載：${formatBytes(networkUi.cellularBytesThisMonth)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()
            Text("預先下載", style = MaterialTheme.typography.titleMedium)
            prefetchProgress?.let { (done, total) ->
                Column {
                    Text("下載中… $done/$total", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } ?: Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { requestPrefetch(PrefetchScope.NORMAL_ONLY) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("預先下載卡圖（僅普卡，約 ${cardRepo.snapshot.cards.size} 張）") }
                OutlinedButton(
                    onClick = { requestPrefetch(PrefetchScope.ALL_PRINTINGS) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("預先下載卡圖（含全部刷版，約 ${allPrintings.size} 張）") }
            }
            Text(
                "建議出門前先在家用 Wi-Fi 下載完成，牌店現場網路不好時仍能看圖。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text("圖片快取", style = MaterialTheme.typography.titleMedium)
            LabeledRow("已快取卡圖", "$cachedCount 張")
            LabeledRow("快取容量", formatBytes(cacheSize))
            TextButton(
                onClick = { confirmClearCache = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("清除圖片快取") }
            Text(
                "卡圖存於本機（不佔系統備份），看過一次即永久保留。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text("App 版本", style = MaterialTheme.typography.titleMedium)
            LabeledRow("目前版本", "v${appUpdater.currentVersionName()}")
            when (val s = appUpdateState) {
                is AppUpdater.State.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("檢查中…", style = MaterialTheme.typography.bodyMedium)
                }
                is AppUpdater.State.Downloading -> Column {
                    Text("下載中… ${s.done}/${s.total}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (s.total == 0) 0f else s.done.toFloat() / s.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is AppUpdater.State.UpdateAvailable -> Button(
                    onClick = { scope.launch { appUpdater.downloadAndInstall(s.downloadUrl) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("更新到 ${s.versionName}") }
                is AppUpdater.State.UpToDate -> Column {
                    Text(
                        "已是最新版本",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { scope.launch { appUpdater.check(silent = false) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("重新檢查") }
                }
                is AppUpdater.State.Failed -> Column {
                    Text(s.message, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { scope.launch { appUpdater.check(silent = false) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("重試") }
                }
                AppUpdater.State.Idle -> OutlinedButton(
                    onClick = { scope.launch { appUpdater.check(silent = false) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("檢查 App 更新") }
            }

            HorizontalDivider()
            Surface(
                onClick = { onboarding.restart() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("幫助", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                "忘了怎麼使用嗎？點這裡重新看一次新手教學。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    confirmPrefetch?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmPrefetch = null },
            title = { Text("目前使用行動網路") },
            text = {
                Column {
                    Text("預先下載將消耗約 ${formatBytes(estimateSize(printingsFor(target)))} 流量")
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            confirmPrefetch = null
                            networkPolicy.queuePrefetchForWiFi(printingsFor(target))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("僅用 Wi-Fi 時下載") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmPrefetch = null
                    startPrefetch(target)
                }) { Text("仍要下載") }
            },
            dismissButton = {
                TextButton(onClick = { confirmPrefetch = null }) { Text("取消") }
            },
        )
    }

    if (confirmClearCache) {
        AlertDialog(
            onDismissRequest = { confirmClearCache = false },
            title = { Text("清除圖片快取？") },
            text = { Text("確定要清除全部卡圖快取嗎？之後需要重新下載。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearCache = false
                    ImageCacheOps.clearCache(context)
                    scope.launch { refreshCacheInfo() }
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearCache = false }) { Text("取消") }
            },
        )
    }

}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

private enum class PrefetchScope { NORMAL_ONLY, ALL_PRINTINGS }

/** 單張約 110 KB 估算，跟 iOS SettingsView.estimateSize(for:) 同一個粗估值 */
private fun estimateSize(printings: List<Printing>): Long = printings.size.toLong() * 110 * 1024

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
