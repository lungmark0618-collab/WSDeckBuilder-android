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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
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

@Composable
private fun AccentSwatchRow(selected: AccentPreset, onSelect: (AccentPreset) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(preset.color)
                    .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
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
