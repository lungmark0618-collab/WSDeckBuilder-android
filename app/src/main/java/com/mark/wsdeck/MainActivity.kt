package com.mark.wsdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.data.DeckRepository
import com.mark.wsdeck.ui.browser.CatalogScreen
import com.mark.wsdeck.ui.deck.DeckDetailScreen
import com.mark.wsdeck.ui.deck.DeckListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cardRepo = CardRepository(applicationContext)
        val deckRepo = DeckRepository(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot(cardRepo, deckRepo)
                }
            }
        }
    }
}

private sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Catalog : Tab("catalog", "圖鑑", Icons.Filled.Search)
    object Decks : Tab("decks", "牌組", Icons.Filled.Style)
}
private val tabs = listOf(Tab.Catalog, Tab.Decks)

@Composable
private fun AppRoot(cardRepo: CardRepository, deckRepo: DeckRepository) {
    // null = 還在載入。5.7 MB 的 JSON 要解一下，先蓋住空畫面
    var loaded by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { loaded = cardRepo.load() }

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
        true -> MainScaffold(cardRepo, deckRepo)
    }
}

@Composable
private fun MainScaffold(cardRepo: CardRepository, deckRepo: DeckRepository) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val currentDestination = backStack?.destination
            // 牌組詳情頁沒有自己的分頁列項目，靠上一頁的返回鍵回去，
            // 但底部列仍要顯示、且判斷「牌組」分頁為選取狀態
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == tab.route || (tab == Tab.Decks && it.route == "deck/{uuid}")
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Catalog.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Catalog.route) { CatalogScreen(cardRepo, deckRepo) }
            composable(Tab.Decks.route) {
                DeckListScreen(cardRepo, deckRepo) { uuid -> navController.navigate("deck/$uuid") }
            }
            composable("deck/{uuid}") { backStackEntry ->
                val uuid = backStackEntry.arguments?.getString("uuid") ?: return@composable
                DeckDetailScreen(uuid, cardRepo, deckRepo) { navController.popBackStack() }
            }
        }
    }
}
