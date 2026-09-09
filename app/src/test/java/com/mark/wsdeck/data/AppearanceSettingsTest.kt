package com.mark.wsdeck.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceSettingsTest {
    @Test fun `selected background persists and invalid custom color is ignored`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = AppearanceSettings(context)
        settings.setBackground(BackgroundStyle.CUSTOM)
        settings.setCustomBackgroundHex("#12ab34")
        settings.setCustomBackgroundHex("invalid")
        val reloaded = AppearanceSettings(context).ui.value
        assertEquals(BackgroundStyle.CUSTOM, reloaded.background)
        assertEquals("12AB34", reloaded.customBackgroundHex)
    }
}
