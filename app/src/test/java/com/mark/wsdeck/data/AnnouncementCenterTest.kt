package com.mark.wsdeck.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnnouncementCenterTest {
    @Test fun sourcesDeduplicateAndReadStatePersistsPerItem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = Prefs(context)
        val first = Announcement("data-update-A-2", "2026-09-11", "新系列", "可下載")
        val second = Announcement("manual", "2026-09-11", "公告", "內容")
        prefs.cachedAnnouncements = listOf(first, second)
        prefs.cachedLocalAnnouncements = listOf(first)
        prefs.announcementReadIds = emptySet()
        prefs.announcementDeletedIds = emptySet()
        val policy = NetworkPolicy(context)
        val center = AnnouncementCenter(context, policy)
        assertEquals(2, center.ui.value.unreadCount)
        center.markRead(first)
        assertEquals(1, center.ui.value.unreadCount)
        assertEquals(1, AnnouncementCenter(context, policy).ui.value.unreadCount)
        center.delete(first)
        assertEquals(listOf(second), AnnouncementCenter(context, policy).ui.value.items)
    }

    @Test fun keepsHighestNumericVersionNotLexicographicVersion() {
        val items = listOf(2, 10, 3).map { Announcement("data-update-A-B-$it", "2026-09-11", "更新", "內容") }
        assertEquals(listOf("data-update-A-B-10"), compactLocalAnnouncements(items).map { it.id })
    }
}
