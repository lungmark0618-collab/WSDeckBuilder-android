package com.mark.wsdeck.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 行動數據控制，對應 iOS 的 NetworkPolicy：是否用行動數據下載卡圖，交由使用者決定。
 * 用 ConnectivityManager 的 NetworkCallback 持續監看，對應 iOS 的 NWPathMonitor。
 */
class NetworkPolicy(context: Context) {

    enum class Mode {
        ALWAYS,          // 一律允許
        WIFI_ONLY,       // 僅 Wi-Fi（預設）
        WIFI_ONLY_MANUAL; // 僅 Wi-Fi，但可單張載入

        val label: String
            get() = when (this) {
                ALWAYS -> "一律允許"
                WIFI_ONLY -> "僅 Wi-Fi"
                WIFI_ONLY_MANUAL -> "僅 Wi-Fi，但可單張載入"
            }
        val detail: String
            get() = when (this) {
                ALWAYS -> "Wi-Fi 與行動數據都直接下載"
                WIFI_ONLY -> "行動網路下不下載，顯示佔位圖"
                WIFI_ONLY_MANUAL -> "行動網路下點一下佔位圖可只載那一張"
            }
    }

    data class UiState(
        val mode: Mode = Mode.WIFI_ONLY,
        val isExpensive: Boolean = false,     // 行動網路或個人熱點
        val isConstrained: Boolean = false,   // 系統「資料節省模式」
        val isConnected: Boolean = true,
        val cellularBytesThisMonth: Long = 0L,
    ) {
        /** 自動下載是否允許（捲動圖鑑時） */
        val allowsAutomaticDownload: Boolean
            get() {
                if (!isConnected) return false
                if (isConstrained) return false
                if (!isExpensive) return true
                return mode == Mode.ALWAYS
            }

        /** 使用者主動點擊佔位圖時是否放行 */
        val allowsManualDownload: Boolean
            get() {
                if (!isConnected) return false
                if (!isExpensive) return true
                return mode != Mode.WIFI_ONLY
            }

        /** 行動網路下批次預載前必須確認 */
        val prefetchNeedsConfirmation: Boolean get() = isExpensive
    }

    private val prefs = Prefs(context)
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _ui = MutableStateFlow(
        UiState(
            mode = prefs.networkMode,
            cellularBytesThisMonth = rolledOverBytes(),
        ),
    )
    val ui: StateFlow<UiState> = _ui

    init {
        refreshFromActiveNetwork()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshFromActiveNetwork()
            override fun onLost(network: Network) = refreshFromActiveNetwork()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                refreshFromActiveNetwork()
        })
    }

    fun setMode(mode: Mode) {
        prefs.networkMode = mode
        _ui.update { it.copy(mode = mode) }
    }

    fun recordDownload(bytes: Int, viaExpensivePath: Boolean) {
        if (!viaExpensivePath) return
        val total = rolledOverBytes() + bytes
        prefs.cellularBytesThisMonth = total
        _ui.update { it.copy(cellularBytesThisMonth = total) }
    }

    private fun refreshFromActiveNetwork() {
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val expensive = caps == null ||
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val constrained = cm?.restrictBackgroundStatus ==
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        _ui.update {
            it.copy(isConnected = connected, isExpensive = expensive, isConstrained = constrained)
        }
    }

    /** 月份變了就歸零，對應 iOS 的 rolloverStatsIfNeeded() */
    private fun rolledOverBytes(): Long {
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(java.util.Date())
        if (prefs.cellularStatsMonth != month) {
            prefs.cellularStatsMonth = month
            prefs.cellularBytesThisMonth = 0L
        }
        return prefs.cellularBytesThisMonth
    }
}
