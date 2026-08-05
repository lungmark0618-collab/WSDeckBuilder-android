package com.mark.wsdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.data.CardRepository
import com.mark.wsdeck.ui.browser.CatalogScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = CardRepository(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot(repo)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(repo: CardRepository) {
    // null = 還在載入。5.7 MB 的 JSON 要解一下，先蓋住空畫面
    var loaded by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { loaded = repo.load() }

    when (loaded) {
        null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(14.dp))
                Text("載入卡片資料…", style = MaterialTheme.typography.bodySmall)
            }
        }
        false -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text(repo.loadError ?: "資料載入失敗")
        }
        true -> CatalogScreen(repo)
    }
}
