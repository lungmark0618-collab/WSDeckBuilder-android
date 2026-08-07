package com.mark.wsdeck.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.CardDataStore
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.DataUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 設定：卡表線上更新，對應 iOS SettingsView 的「卡表」區塊。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(cardRepo: CardRepository, updater: DataUpdater) {
    val ui by updater.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showRevertConfirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("設定") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("卡表資料", style = MaterialTheme.typography.titleMedium)

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

                is DataUpdater.State.UpdateAvailable -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    s.pending.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(p.titleName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (p.fromVersion == 0) "新作品 v${p.toVersion}"
                                else "v${p.fromVersion} → v${p.toVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { scope.launch { updater.performUpdate(s.pending, cardRepo) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("更新 ${s.pending.size} 部作品的卡表") }
                }

                else -> Button(
                    onClick = { scope.launch { updater.check(cardRepo, silent = false) } },
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

            if (CardDataStore.hasDownloadedData(context)) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                TextButton(
                    onClick = { showRevertConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("還原為 App 內建卡表") }
            }
        }
    }

    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            title = { Text("還原為 App 內建卡表？") },
            text = { Text("已下載的卡表更新會被刪除，改用這次安裝內建的版本。") },
            confirmButton = {
                TextButton(onClick = {
                    showRevertConfirm = false
                    scope.launch { updater.revertToBundled(cardRepo) }
                }) { Text("還原", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirm = false }) { Text("取消") }
            },
        )
    }
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
