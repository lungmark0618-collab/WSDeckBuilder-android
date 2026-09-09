package com.mark.wsdeck.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mark.wsdeck.data.*

/**
 * 外觀設定，對應 iOS 的 AppearanceSettingsView：字級、字重、文字色、背景、強調色。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(appearance: AppearanceSettings, onboarding: OnboardingState, onBack: () -> Unit) {
    val ui by appearance.ui.collectAsStateWithLifecycle()

    // 引導教學：進到這頁等於完成了「外觀設定」這一步，對應 iOS 的 .onAppear
    LaunchedEffect(Unit) { onboarding.notify(OnboardingStep.APPEARANCE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外觀") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingGroup("背景") {
                SegmentedRow(BackgroundStyle.entries, ui.background, { it.label }, appearance::setBackground)
                if (ui.background == BackgroundStyle.CUSTOM) {
                    CustomBackgroundPicker(ui.customBackgroundHex, appearance::setCustomBackgroundHex)
                }
                Text("立即套用所有分頁，文字與面板會隨背景明暗調整。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SettingGroup("預覽") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("你的卡牌收藏", style = MaterialTheme.typography.titleMedium)
                        Text("清楚的文字，舒服的閱讀。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("50 / 50 張", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            SettingGroup("字級") {
                SegmentedRow(TextSize.entries, ui.textSize, { it.label }, appearance::setTextSize)
            }
            SettingGroup("字重") {
                SegmentedRow(TextWeightOption.entries, ui.textWeight, { it.label }, appearance::setTextWeight)
            }
            SettingGroup("文字色") {
                SegmentedRow(TextTone.entries, ui.textTone, { it.label }, appearance::setTextTone)
            }
            SettingGroup("強調色") {
                SegmentedRow(AccentMode.entries, ui.accentMode, { it.label }, appearance::setAccentMode)
                if (ui.accentMode == AccentMode.FIXED) {
                    Spacer(Modifier.height(8.dp))
                    AccentSwatchRow(ui.fixedAccent, appearance::setFixedAccent)
                }
            }
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatchRow(selected: AccentPreset, onSelect: (AccentPreset) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(preset.color)
                    .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                    .semantics { contentDescription = preset.label }
                    .clickable { onSelect(preset) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = preset.label, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CustomBackgroundPicker(hex: String, onChange: (String) -> Unit) {
    var input by remember(hex) { mutableStateOf(hex) }
    val value = hex.toLongOrNull(16) ?: 0xE8E4DC
    val channels = listOf(((value shr 16) and 255).toInt(), ((value shr 8) and 255).toInt(), (value and 255).toInt())
    OutlinedTextField(
        value = input,
        onValueChange = { text ->
            input = text.removePrefix("#").take(6).uppercase()
            if (input.matches(Regex("[0-9A-F]{6}"))) onChange(input)
        },
        label = { Text("背景色碼") }, prefix = { Text("#") }, singleLine = true,
        isError = !input.matches(Regex("[0-9A-F]{6}")),
        supportingText = { Text("六位十六進位色碼，例如 E8E4DC") },
        modifier = Modifier.fillMaxWidth(),
    )
    listOf("紅", "綠", "藍").forEachIndexed { index, label ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.width(32.dp))
            Slider(
                value = channels[index].toFloat(),
                onValueChange = { next ->
                    val updated = channels.toMutableList()
                    updated[index] = next.toInt()
                    onChange(updated.joinToString("") { "%02X".format(it) })
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f).semantics { contentDescription = "背景${label}色" },
            )
            Text("${channels[index]}", modifier = Modifier.width(32.dp))
        }
    }
}
