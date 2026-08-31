package com.mark.wsdeck.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
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

/**
 * App 本體更新：這個 App 沒上架 Play 商店，直接發布 APK，所以自己對
 * GitHub Releases 查有沒有新版——release 的 tag 固定用「vN」（N 是
 * versionCode 整數），跟目前安裝的版本比大小即可，不用猜版號字串。
 */
class AppUpdater(private val context: Context) {

    sealed class State {
        object Idle : State()
        object Checking : State()
        object UpToDate : State()
        data class UpdateAvailable(val versionName: String, val notes: String, val downloadUrl: String) : State()
        data class Downloading(val done: Int, val total: Int) : State()
        data class Failed(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val client = OkHttpClient()

    companion object {
        private const val RELEASES_URL =
            "https://api.github.com/repos/lungmark0618-collab/WSDeckBuilder-android/releases/latest"
    }

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tagName: String,
        val name: String? = null,
        val body: String? = null,
        val assets: List<Asset> = emptyList(),
    )

    @Serializable
    private data class Asset(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )

    /**
     * silent=true（冷啟動自動查）：查不到、解不開、沒有更新，一律安靜放棄，不顯示
     * 檢查中／已是最新版這類過程狀態，只有真的有更新才彈窗。
     * silent=false（設定頁手動按「檢查更新」）：使用者主動要結果，檢查中、已是最新、
     * 失敗訊息都要秀出來，不能就這樣沒反應。
     */
    suspend fun check(silent: Boolean) = withContext(Dispatchers.IO) {
        if (!silent) _state.update { State.Checking }
        try {
            val request = Request.Builder().url(RELEASES_URL).build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (!silent) _state.update { State.Failed("查詢失敗（${response.code}）") }
                    return@use null
                }
                response.body?.string()
            } ?: return@withContext
            val release = cardJson.decodeFromString<Release>(body)
            val versionCode = release.tagName.removePrefix("v").toIntOrNull()
            if (versionCode == null) {
                if (!silent) _state.update { State.Failed("版本資訊格式不正確") }
                return@withContext
            }
            if (versionCode <= currentVersionCode()) {
                if (!silent) _state.update { State.UpToDate }
                return@withContext
            }
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
            if (apk == null) {
                if (!silent) _state.update { State.Failed("找不到可下載的安裝檔") }
                return@withContext
            }
            _state.update {
                State.UpdateAvailable(
                    versionName = release.name ?: release.tagName,
                    notes = release.body.orEmpty(),
                    downloadUrl = apk.browserDownloadUrl,
                )
            }
        } catch (e: Exception) {
            if (!silent) _state.update { State.Failed("檢查更新失敗：${e.message ?: e.toString()}") }
        }
    }

    private fun currentVersionCode(): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION") info.versionCode
        }
    }

    fun currentVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    suspend fun downloadAndInstall(downloadUrl: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body
                if (!response.isSuccessful || responseBody == null) {
                    _state.update { State.Failed("下載失敗（${response.code}）") }
                    return@withContext
                }
                val total = responseBody.contentLength().let { if (it > 0) it.toInt() else 0 }
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(dir, "wsdeck-update.apk")
                responseBody.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            done += read
                            _state.update { State.Downloading(done, total) }
                        }
                    }
                }
                val launched = withContext(Dispatchers.Main) { promptInstall(target) }
                if (launched) _state.update { State.Idle }
            }
        } catch (e: IOException) {
            _state.update { State.Failed("下載失敗：${e.message ?: e.toString()}") }
        }
    }

    /** 沒上架商店的 APK 得先過「安裝不明來源」這關，沒開就導去系統設定讓使用者自己開；
     *  回傳有沒有真的跳出安裝畫面，沒有的話呼叫端不該把 Failed 狀態蓋掉 */
    private fun promptInstall(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            _state.update {
                State.Failed("請先允許安裝不明來源的應用程式，然後回來重新按一次更新。")
            }
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    fun dismiss() {
        _state.update { State.Idle }
    }
}
