package com.mark.wsdeck.ui.deck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.data.DeckEntryEntity
import com.mark.wsdeck.data.DeckImageExporter

/**
 * 面對面分享牌組：直接把 QR 顯示在螢幕上讓朋友的手機掃，不用先出圖存檔、
 * 再傳來傳去佔空間——打牌現場最實用的分享方式。對應 iOS 的 DeckQRPresentView。
 */
@Composable
fun DeckQRPresentDialog(
    deckName: String,
    entries: List<DeckEntryEntity>,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(deckName, entries) {
        DeckImageExporter.qrBitmap(DeckImageExporter.Payload.encode(deckName, entries), 720)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("出示 QR", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap.asImageBitmap(),
                        contentDescription = "牌組 QR",
                        modifier = Modifier
                            .size(260.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    )
                } else {
                    Text("這副牌組還沒有卡")
                }
                Spacer(Modifier.height(12.dp))
                Text(deckName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "讓朋友直接用相機掃這個畫面，或用他 App 裡的掃描功能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}
