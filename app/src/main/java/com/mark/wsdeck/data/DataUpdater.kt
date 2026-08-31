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
import java.io.IOException
import java.net.URL

/**
 * 卡表線上更新，對應 iOS 的 DataUpdater。同一份 manifest.json、同一套
 * schema_version／data_version 比較邏輯，兩邊讀的是同一個發佈來源。
 *
 * 下載下來的檔案放在 noBackupFilesDir（不算進系統自動備份，卡表隨時可
 * 從網路重建，沒必要佔備份額度——跟 iOS 端 Application Support 目錄設
 * isExcludedFromBackup 是同樣的考量），檔名對得上就覆蓋內建 assets 版本，
 * 對不上（還沒下載過）就繼續用內建版本。
 */
object CardDataStore {
    fun directory(context: Context): File =
        File(context.noBackupFilesDir, "CardData").apply { mkdirs() }

}

@Serializable
data class UpdateManifest(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("updated_at") val updatedAt: String? = null,
    val notes: String? = null,
    val sets: List<SetEntry> = emptyList(),
) {
    @Serializable
    data class SetEntry(
        @SerialName("title_code") val titleCode: String,
        /** 這部作品第一次出現（本機還沒下載過卡表）時，通知要顯示中文名稱
         *  只能靠這個——本機資料庫查不到還沒下載過的作品，對應 iOS 同名欄位 */
        @SerialName("title_name_zh") val titleNameZH: String? = null,
        val file: String,
        @SerialName("data_version") val dataVersion: Int,
        val url: String,
    )
}

class DataUpdater(private val context: Context, private val networkPolicy: NetworkPolicy) {

    sealed class State {
        object Idle : State()
        object Checking : State()
        data class UpdateAvailable(val pending: List<Pending>) : State()
        data class Downloading(val done: Int, val total: Int) : State()
        object UpToDate : State()
        data class Failed(val message: String) : State()
    }

    data class Pending(
        val titleCode: String,
        val titleName: String,
        val file: String,
        val url: String,
        val fromVersion: Int,
        val toVersion: Int,
    )

    data class UiState(
        val state: State = State.Idle,
        val notes: String? = null,
        val lastCheckedAt: Long? = null,
    )

    private val prefs = Prefs(context)
    private val client = OkHttpClient()

    private val _ui = MutableStateFlow(
        UiState(lastCheckedAt = prefs.cardDataLastCheckedAt.takeIf { it > 0 }),
    )
    val ui: StateFlow<UiState> = _ui

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/lungmark0618-collab/WSDeckBuilder-data/main/manifest.json"
    }

    /**
     * App 啟動時呼叫：查不到就沿用本地資料，不跳錯誤打擾（跟 iOS 同樣的原則）。
     *
     * 原本有「一天查一次」的節流，但這樣使用者得自己按「檢查更新」才看得到通知，
     * 想要的其實是「開 App 就自動查、有新的話通知自己跳出來」。manifest.json
     * 就幾 KB、放在 GitHub raw CDN 後面，每次開 App 都查一次完全負擔得起。
     */
    suspend fun checkSilently(cardRepo: CardRepository) {
        if (!networkPolicy.ui.value.allowsAutomaticDownload) return
        check(cardRepo, silent = true)
    }

    suspend fun check(cardRepo: CardRepository, silent: Boolean) {
        if (!silent) _ui.update { it.copy(state = State.Checking) }
        try {
            val manifest = fetchManifest()
            if (manifest.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
                _ui.update {
                    it.copy(state = State.Failed("這份卡表清單版本比較新，請先更新 App 本身"))
                }
                return
            }
            val now = System.currentTimeMillis()
            prefs.cardDataLastCheckedAt = now
            val pending = computePending(manifest, MANIFEST_URL, cardRepo.snapshot.sets)
            _ui.update {
                it.copy(
                    state = if (pending.isEmpty()) State.UpToDate else State.UpdateAvailable(pending),
                    notes = manifest.notes,
                    lastCheckedAt = now,
                )
            }
        } catch (e: Exception) {
            val message = messageFor(e)
            _ui.update { it.copy(state = if (silent) State.Idle else State.Failed(message)) }
        }
    }

    suspend fun performUpdate(pending: List<Pending>, cardRepo: CardRepository) {
        if (pending.isEmpty()) return
        if (_ui.value.state is State.Downloading) return
        _ui.update { it.copy(state = State.Downloading(done = 0, total = pending.size)) }

        var installed = 0
        for (item in pending) {
            try {
                install(item)
                installed++
                _ui.update { it.copy(state = State.Downloading(done = installed, total = pending.size)) }
            } catch (e: Exception) {
                if (installed > 0) cardRepo.reload()
                _ui.update { it.copy(state = State.Failed("${item.titleName}：${messageFor(e)}")) }
                return
            }
        }
        cardRepo.reload()
        _ui.update { it.copy(state = State.UpToDate) }
    }

    private suspend fun install(item: Pending) = withContext(Dispatchers.IO) {
        val bytes = fetchBytes(item.url)
        networkPolicy.recordDownload(bytes = bytes.size, viaExpensivePath = networkPolicy.ui.value.isExpensive)
        val decoded = cardJson.decodeFromString<CardSet>(String(bytes))
        if (decoded.meta.titleCode != item.titleCode) {
            throw IOException("下載的內容跟預期的作品對不上")
        }
        if (decoded.cards.isEmpty()) {
            throw IOException("下載的檔案裡沒有任何卡片")
        }
        val dir = CardDataStore.directory(context)
        val temp = File(dir, "${item.file}.download")
        temp.writeBytes(bytes)
        val target = File(dir, item.file)
        if (target.exists()) target.delete()
        // 同目錄內搬移在同一個檔案系統上是原子操作，不會留下寫一半的檔案
        if (!temp.renameTo(target)) throw IOException("寫入卡表檔案失敗")
    }

    private suspend fun fetchManifest(): UpdateManifest {
        val bytes = fetchBytes(MANIFEST_URL)
        return cardJson.decodeFromString(String(bytes))
    }

    private suspend fun fetchBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            // manifest 版號一改就要立刻看到，不能被任何一層快取擋住
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("伺服器回應 ${response.code}")
            response.body?.bytes() ?: throw IOException("沒有回應內容")
        }
    }

    private fun messageFor(e: Exception): String = when (e) {
        is kotlinx.serialization.SerializationException -> "檔案格式不正確，無法解析"
        else -> e.message ?: e.toString()
    }
}

/**
 * manifest 裡哪些作品有更新可裝：本地沒有的視為版本 0（新作品直接算作有更新），
 * 嚴格大於才算，純函式方便測試（對應 iOS 的 DataUpdater.pending(in:base:database:)）。
 */
fun computePending(
    manifest: UpdateManifest,
    manifestUrl: String,
    localSets: List<CardSetMeta>,
): List<DataUpdater.Pending> {
    val localByTitle = localSets.associateBy { it.titleCode }
    return manifest.sets.mapNotNull { entry ->
        if (!entry.file.endsWith("_cards.json")) return@mapNotNull null
        val resolvedUrl = try {
            URL(URL(manifestUrl), entry.url).toString()
        } catch (e: Exception) {
            return@mapNotNull null
        }
        val local = localByTitle[entry.titleCode]
        val current = local?.dataVersion ?: 0
        if (entry.dataVersion <= current) return@mapNotNull null
        DataUpdater.Pending(
            titleCode = entry.titleCode,
            titleName = local?.titleNameZH ?: entry.titleNameZH ?: entry.titleCode,
            file = entry.file,
            url = resolvedUrl,
            fromVersion = current,
            toVersion = entry.dataVersion,
        )
    }
}
