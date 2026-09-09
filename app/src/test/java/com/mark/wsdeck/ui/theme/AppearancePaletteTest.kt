package com.mark.wsdeck.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mark.wsdeck.data.AppearanceSettings
import com.mark.wsdeck.data.BackgroundStyle
import org.junit.Assert.*
import org.junit.Test

class AppearancePaletteTest {
    @Test fun `system style follows device while explicit style stays fixed`() {
        val system = AppearanceSettings.UiState(background = BackgroundStyle.SYSTEM)
        assertNotEquals(appearanceColorScheme(system, true).background, appearanceColorScheme(system, false).background)
        val paper = system.copy(background = BackgroundStyle.PAPER)
        assertEquals(appearanceColorScheme(paper, true).background, appearanceColorScheme(paper, false).background)
    }

    @Test fun `custom backgrounds keep exact color and readable primary text`() {
        for (hex in listOf("000000", "FFFFFF", "E8E4DC", "FF0000", "00FF00", "0000FF", "777777", "707070")) {
            val scheme = appearanceColorScheme(AppearanceSettings.UiState(background = BackgroundStyle.CUSTOM, customBackgroundHex = hex), false)
            assertEquals(Color(0xFF000000L or hex.toLong(16)), scheme.background)
            for (surface in listOf(scheme.background, scheme.surface, scheme.surfaceContainerHigh)) {
                val a = surface.luminance()
                val b = scheme.onSurface.luminance()
                assertTrue("$hex contrast", (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f) >= 4.5f)
            }
        }
    }
}
