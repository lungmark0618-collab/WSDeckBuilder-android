package com.mark.wsdeck.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.mark.wsdeck.data.OnboardingState
import com.mark.wsdeck.data.OnboardingStep

/**
 * 標記「這個元件是教學某一步要指的目標」，對應 iOS 的 .onboardingAnchor(_:)。
 * 用 boundsInRoot 而不是 boundsInWindow——疊層也畫在同一個 root Box 裡，
 * 座標系統一致，不用再算狀態列高度之類的落差。
 */
fun Modifier.onboardingAnchor(step: OnboardingStep, onboarding: OnboardingState): Modifier =
    this.onGloballyPositioned { coordinates ->
        onboarding.anchors[step] = coordinates.boundsInRoot()
    }

/**
 * 教學疊層本體：目標元件周圍的光圈 + 浮動提示卡，對應 iOS 的 OnboardingOverlay。
 * 刻意不擋任何點擊（沒有全螢幕的深色遮罩）——使用者要切分頁、要點別的地方，
 * 教學不應該擋路，這是「直接操作」教學跟傳統強制性 modal 導覽最大的差異。
 *
 * Compose 沒有原生的「這層以下關掉點擊、疊上去的新圖層照樣能點」語意，所以光圈
 * 本身乾脆不掛任何 clickable／pointerInput，天生就不會攔截點擊；提示卡的按鈕
 * 是額外疊上去的獨立 Box，本來就收得到點擊。
 */
@Composable
fun OnboardingOverlay(onboarding: OnboardingState) {
    val step = onboarding.currentStep ?: return
    val rect = onboarding.anchors[step]

    Box(Modifier.fillMaxSize()) {
        rect?.let { Spotlight(it) }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                // 底部導覽列會蓋住提示卡，墊高避開
                .padding(bottom = 96.dp),
        ) {
            TooltipCard(step, onboarding)
        }
    }
}

@Composable
private fun Spotlight(rect: Rect) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    with(density) {
        Box(
            Modifier
                .offset(x = (rect.left - 8).toDp(), y = (rect.top - 8).toDp())
                .size((rect.width + 16).toDp(), (rect.height + 16).toDp())
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
        )
    }
}

@Composable
private fun TooltipCard(step: OnboardingStep, onboarding: OnboardingState) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(step.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            step.displayIndex?.let { index ->
                Text(
                    "$index / ${OnboardingStep.countedTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            step.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onboarding.skip() }) { Text("跳過教學") }
            Spacer(Modifier.weight(1f))
            if (step.index > 0) {
                OutlinedButton(onClick = { onboarding.retreat() }) { Text("上一步") }
                Spacer(Modifier.width(8.dp))
            }
            Button(onClick = { onboarding.advance() }) {
                Text(
                    when {
                        step == OnboardingStep.WELCOME -> "開始"
                        step == OnboardingStep.entries.last() -> "完成"
                        else -> "下一步"
                    },
                )
            }
        }
    }
}
