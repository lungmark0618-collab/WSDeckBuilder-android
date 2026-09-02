package com.mark.wsdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mark.wsdeck.data.AnnouncementCenter
import com.mark.wsdeck.data.AppUpdater
import com.mark.wsdeck.data.AppearanceSettings
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.CollectionRepository
import com.mark.wsdeck.data.DataUpdater
import com.mark.wsdeck.data.DeckImageExporter
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.data.FavoriteTitlesStore
import com.mark.wsdeck.data.NetworkPolicy
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingTab
import com.mark.wsdeck.data.WSNewsRepository
import com.mark.wsdeck.ui.browser.CatalogScreen
import com.mark.wsdeck.ui.deck.DeckDetailScreen
import com.mark.wsdeck.ui.deck.DeckListScreen
import com.mark.wsdeck.ui.onboarding.OnboardingOverlay
import com.mark.wsdeck.ui.settings.AppearanceSettingsScreen
import com.mark.wsdeck.ui.settings.SettingsScreen
import com.mark.wsdeck.ui.shared.GlassTabBar
import com.mark.wsdeck.ui.shared.GlassTabBarItem
import com.mark.wsdeck.ui.theme.AppSurface
import kotlinx.coroutines.launch
import com.mark.wsdeck.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    // 朋友用系統相機（不是這個 App 內建的掃描功能）掃分享出去的牌組 QR 時，
    // 靠 wsdeck://import 這個 scheme 喚起這個 Activity——冷啟動走 onCreate
    // 的 intent，App 已經開著時走 onNewIntent，兩邊都要接住同一個狀態
    private var pendingDeepLink by mutableStateOf<android.net.Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLink = intent?.data
        val cardRepo = CardRepository(applicationContext)
        val deckRepo = DeckRepository(applicationContext)
        val collectionRepo = CollectionRepository(applicationContext)
        val networkPolicy = NetworkPolicy(applicationContext)
        val updater = DataUpdater(applicationContext, networkPolicy)
        val appUpdater = AppUpdater(applicationContext)
        val announcements = AnnouncementCenter(applicationContext, networkPolicy)
        val appearance = AppearanceSettings(applicationContext)
        val onboarding = OnboardingState(applicationContext)
        val favorites = FavoriteTitlesStore(applicationContext)
        val newsRepo = WSNewsRepository(applicationContext)
        setContent {
            val appearanceUi by appearance.ui.collectAsStateWithLifecycle()
            AppTheme(appearanceUi) {
                AppRoot(
                    cardRepo, deckRepo, collectionRepo, updater, appUpdater, announcements,
                    appearance, networkPolicy, onboarding, favorites, newsRepo,
                    deepLinkUri = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data
    }
}

private sealed class Tab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    // 首頁沒有對應的教學步驟，null 就好——不能借用 CATALOG，不然下面
    // 反查「這個 onboardingTab 對應哪個 Tab」會因為首頁排在前面而誤配到它
    val onboardingTab: OnboardingTab?,
) {
    object Home : Tab("home", "首頁", Icons.Filled.Home, null)
    object Catalog : Tab("catalog", "圖鑑", Icons.Filled.Search, OnboardingTab.CATALOG)
    object Decks : Tab("decks", "牌組", Icons.Filled.Style, OnboardingTab.DECKS)
    object Settings : Tab("settings", "設定", Icons.Filled.Settings, OnboardingTab.SETTINGS)
}
private val tabs = listOf(Tab.Home, Tab.Catalog, Tab.Decks, Tab.Settings)

@Composable
private fun AppRoot(
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    collectionRepo: CollectionRepository,
    updater: DataUpdater,
    appUpdater: AppUpdater,
    announcements: AnnouncementCenter,
    appearance: AppearanceSettings,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
    favorites: FavoriteTitlesStore,
    newsRepo: WSNewsRepository,
    deepLinkUri: android.net.Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    // null = 還在載入。5.7 MB 的 JSON 要解一下，先蓋住空畫面
    var loaded by remember { mutableStateOf<Boolean?>(null) }

    suspend fun checkForUpdates() {
        updater.checkSilently(cardRepo)
        (updater.ui.value.state as? DataUpdater.State.UpdateAvailable)?.let {
            announcements.noteDataUpdates(it.pending)
        }
        announcements.checkSilently()
    }

    LaunchedEffect(Unit) {
        loaded = cardRepo.load()
        favorites.migrate(cardRepo)
        // 查更新絕不擋開場：卡表載完、畫面已經能用了才在背景問一次（跟 iOS 同樣的順序）
        checkForUpdates()
        // App 本體有沒有新版只在冷啟動查一次就好，彈窗按「稍後」以後同一次
        // 使用過程不會一直跳出來煩人，下次重新開 App 才會再問
        appUpdater.check(silent = true)
    }

    // 只有冷啟動才會跑上面那個 LaunchedEffect(Unit)，使用者切去別的 App 再切
    // 回來（沒有真的把 App 滑掉重開）並不會重新觸發——這才是「還是要手動按
    // 檢查更新」的真正原因，要另外盯 Activity 回到前景（ON_RESUME）才會再查一次
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && loaded == true) {
                scope.launch { checkForUpdates() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (loaded) {
        null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(14.dp))
                Text("載入卡片資料…", style = MaterialTheme.typography.bodySmall)
            }
        }
        false -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text(cardRepo.loadError ?: "資料載入失敗")
        }
        true -> MainScaffold(cardRepo, deckRepo, collectionRepo, updater, appUpdater, announcements, appearance, networkPolicy, onboarding, favorites, newsRepo)
    }

    val appUpdateState by appUpdater.state.collectAsStateWithLifecycle()
    when (val s = appUpdateState) {
        is AppUpdater.State.UpdateAvailable -> AlertDialog(
            onDismissRequest = { appUpdater.dismiss() },
            title = { Text("有新版本可用") },
            text = {
                Column {
                    Text("版本 ${s.versionName}", style = MaterialTheme.typography.bodyMedium)
                    if (s.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(s.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { appUpdater.downloadAndInstall(s.downloadUrl) } }) {
                    Text("更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { appUpdater.dismiss() }) { Text("稍後") }
            },
        )
        is AppUpdater.State.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("下載更新中…") },
            text = {
                if (s.total > 0) {
                    LinearProgressIndicator(
                        progress = { s.done.toFloat() / s.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {},
        )
        is AppUpdater.State.Failed -> AlertDialog(
            onDismissRequest = { appUpdater.dismiss() },
            title = { Text("更新失敗") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = { appUpdater.dismiss() }) { Text("好") } },
        )
        AppUpdater.State.Idle, AppUpdater.State.Checking, AppUpdater.State.UpToDate -> {}
    }

    // 朋友用系統相機掃分享出去的牌組 QR 時，App 靠 wsdeck:// 連結被喚起，
    // 不管當下停在哪個分頁都要能跳出預覽，所以掛在根層而不是牌組分頁裡
    val parsedImport = remember(deepLinkUri) {
        deepLinkUri?.let { DeckImageExporter.Payload.decode(it.toString()) }
    }
    if (deepLinkUri != null && parsedImport == null) {
        AlertDialog(
            onDismissRequest = onDeepLinkConsumed,
            title = { Text("無法辨識連結") },
            text = { Text("這個連結不是本 App 的牌組分享連結。") },
            confirmButton = { TextButton(onClick = onDeepLinkConsumed) { Text("好") } },
        )
    } else if (parsedImport != null) {
        com.mark.wsdeck.ui.deck.DeckImportPreviewDialog(
            parsed = parsedImport, cardRepo = cardRepo, deckRepo = deckRepo,
            networkPolicy = networkPolicy, onDismiss = onDeepLinkConsumed,
        )
    }
}

@Composable
private fun MainScaffold(
    cardRepo: CardRepository,
    deckRepo: DeckRepository,
    collectionRepo: CollectionRepository,
    updater: DataUpdater,
    appUpdater: AppUpdater,
    announcements: AnnouncementCenter,
    appearance: AppearanceSettings,
    networkPolicy: NetworkPolicy,
    onboarding: OnboardingState,
    favorites: FavoriteTitlesStore,
    newsRepo: WSNewsRepository,
) {
    val navController = rememberNavController()

    // 每一步該在哪個分頁，教學自己切過去——不然從「設定」按幫助重新開始教學，
    // 第一步「搜尋卡片」會卡在設定頁，找不到搜尋列，對應 iOS RootTabView 同段邏輯
    LaunchedEffect(onboarding.currentStep) {
        val targetTab = onboarding.currentStep?.tab ?: return@LaunchedEffect
        val target = tabs.first { it.onboardingTab == targetTab }
        navController.navigate(target.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize().background(AppSurface.background)) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val currentDestination = backStack?.destination
            // 牌組詳情頁沒有自己的分頁列項目，靠上一頁的返回鍵回去，
            // 但底部列仍要顯示、且判斷「牌組」分頁為選取狀態
            val selectedTab = tabs.firstOrNull { tab ->
                currentDestination?.hierarchy?.any {
                    it.route == tab.route || (tab == Tab.Decks && it.route == "deck/{uuid}")
                } == true
            } ?: Tab.Home
            GlassTabBar(
                items = tabs.map { GlassTabBarItem(it, it.label, it.icon) },
                selected = selectedTab,
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route) {
                com.mark.wsdeck.ui.home.HomeScreen(newsRepo, announcements, onboarding)
            }
            composable(Tab.Catalog.route) {
                CatalogScreen(cardRepo, deckRepo, collectionRepo, announcements, appearance, networkPolicy, onboarding, favorites)
            }
            composable(Tab.Decks.route) {
                DeckListScreen(cardRepo, deckRepo, networkPolicy, onboarding) { uuid -> navController.navigate("deck/$uuid") }
            }
            composable("deck/{uuid}") { backStackEntry ->
                val uuid = backStackEntry.arguments?.getString("uuid") ?: return@composable
                DeckDetailScreen(uuid, cardRepo, deckRepo, collectionRepo, networkPolicy) { navController.popBackStack() }
            }
            composable(Tab.Settings.route) {
                SettingsScreen(cardRepo, updater, appUpdater, announcements, appearance, networkPolicy, onboarding) {
                    navController.navigate("settings/appearance")
                }
            }
            composable("settings/appearance") {
                AppearanceSettingsScreen(appearance, onboarding) { navController.popBackStack() }
            }
        }
    }
        OnboardingOverlay(onboarding)
    }
}
