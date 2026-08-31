package com.mark.wsdeck.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.Announcement
import com.mark.wsdeck.data.AnnouncementCenter
import com.mark.wsdeck.data.NotificationBadgeStyle

/**
 * 圖鑑分頁右上角的鈴鐺，開發者發的通知有未讀時顯示紅點或數字（使用者在設定裡選），
 * 對應 iOS 的 NotificationBellButton。
 */
@Composable
fun NotificationBellButton(
    center: AnnouncementCenter,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    val ui by center.ui.collectAsStateWithLifecycle()
    var showList by remember { mutableStateOf(false) }

    Box(modifier) {
        IconButton(onClick = { showList = true; onOpen() }) {
            Icon(
                if (ui.unreadCount > 0) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                contentDescription = "通知",
            )
        }
        if (ui.unreadCount > 0) {
            when (ui.badgeStyle) {
                NotificationBadgeStyle.DOT -> Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                )
                NotificationBadgeStyle.COUNT -> Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (ui.unreadCount > 99) "99+" else "${ui.unreadCount}",
                        color = Color.White,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }

    if (showList) {
        AnnouncementListSheet(center) { showList = false }
    }
}

/** 鈴鐺點開後的通知列表，開場即視為已讀——跟大多數通知中心一樣 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementListSheet(center: AnnouncementCenter, onDismiss: () -> Unit) {
    val ui by center.ui.collectAsStateWithLifecycle()
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { center.markAllRead() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { confirmDeleteAll = true },
                    enabled = ui.items.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("全部刪除") }
                Text("通知", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(64.dp))
            }
            if (ui.items.isEmpty()) {
                Text(
                    "目前沒有通知",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                ui.items.forEach { item ->
                    key(item.id) {
                        AnnouncementRow(item, onDelete = { center.delete(item) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("全部刪除通知？") },
            text = { Text("目前看到的通知都會消失，之後也不會再出現。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    center.deleteAll()
                }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("取消") }
            },
        )
    }
}

/** 看完想清掉就往左滑刪除，對應 iOS 的 .swipeActions——刪除是永久的 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementRow(item: Announcement, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "刪除",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Column {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
