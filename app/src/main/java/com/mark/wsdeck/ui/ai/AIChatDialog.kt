package com.mark.wsdeck.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.ui.shared.SwipeBackDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import com.mark.wsdeck.data.AIChatMessage
import com.mark.wsdeck.data.RulesReference

/** 浮動聊天視窗：問卡牌效果／問規則共用同一個輸入框跟對話串（對應 iOS 的 AIChatSheet） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatDialog(state: AIChatState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("問 AI") },
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "關閉")
                        }
                    },
                )
                state.cardContext?.let { ctx ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "已帶入卡片：${ctx.card.nameZH}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { state.clearCardContext() }) { Text("清除") }
                    }
                }
                if (!RulesReference.isAvailable(context)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "尚未內建規則資料，規則類問題的答案不保證準確",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.messages.isEmpty()) {
                        item { EmptyState() }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        ChatBubble(message)
                    }
                }
                InputBar(state, context, scope)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("可以問什麼？", style = MaterialTheme.typography.titleSmall)
        Text(
            "・這張卡是什麼效果、能不能跟某張卡連動\n・規則問題，例如：安可跟重置的順序、CX combo 怎麼判定",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatBubble(message: AIChatMessage) {
    val isUser = message.role == AIChatMessage.Role.USER
    Row(Modifier.fillMaxWidth()) {
        if (isUser) Spacer(Modifier.weight(1f, fill = false).widthIn(min = 40.dp))
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.isLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    // AI 回答常帶 **粗體** 這類 Markdown 語法，純文字顯示會看到
                    // 一堆星號、像是壞掉——轉成 AnnotatedString 才會是真的粗體
                    Text(
                        boldMarkdown(message.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!isUser) Spacer(Modifier.weight(1f, fill = false).widthIn(min = 40.dp))
    }
}

/** 只處理 `**粗體**`，其餘語法（標題、清單記號）維持原樣顯示，對應 iOS 的
 *  `.inlineOnlyPreservingWhitespace` 範圍 */
private fun boldMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val start = text.indexOf("**", i)
        if (start == -1) { append(text.substring(i)); break }
        val end = text.indexOf("**", start + 2)
        if (end == -1) { append(text.substring(i)); break }
        append(text.substring(i, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(start + 2, end))
        }
        i = end + 2
    }
}

@Composable
private fun InputBar(state: AIChatState, context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.draftText,
            onValueChange = { state.draftText = it },
            placeholder = { Text("輸入問題…") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            maxLines = 4,
        )
        IconButton(
            onClick = { state.send(context, scope) },
            enabled = state.draftText.isNotBlank(),
        ) {
            Icon(
                Icons.Filled.ArrowUpward,
                contentDescription = "送出",
                tint = if (state.draftText.isNotBlank()) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
