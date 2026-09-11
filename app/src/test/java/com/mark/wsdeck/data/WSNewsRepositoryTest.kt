package com.mark.wsdeck.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WSNewsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var requests = 0
    private var status = 200
    private var body = """{"items":[{"date":"2026-09-11","title_jp":"商品","url":"https://example.com/news"}]}"""
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        requests++
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(status).message("test").body(body.toResponseBody()).build()
    }.build()

    @Before fun clearTestCache() {
        File(context.cacheDir, "ws_news_cache.json").delete()
    }

    @Test fun automaticRefreshIsThrottledButManualRefreshBypassesIt() = runBlocking {
        val repo = WSNewsRepository(context, client)
        repo.refresh(force = false)
        repo.refresh(force = false)
        assertEquals(1, requests)
        repo.refresh()
        assertEquals(2, requests)
        val reloaded = WSNewsRepository(context, client)
        reloaded.refresh(force = false)
        assertEquals(2, requests)
        assertEquals("商品", reloaded.ui.value.items.single().titleJP)
    }

    @Test fun serverFailureAndEmptyFeedPreserveLastGoodData() = runBlocking {
        val repo = WSNewsRepository(context, client)
        repo.refresh()
        status = 503
        repo.refresh()
        assertEquals(1, repo.ui.value.items.size)
        assertNotNull(repo.ui.value.errorMessage)
        assertFalse(repo.ui.value.isLoading)
        status = 200
        body = """{"items":[]}"""
        repo.refresh()
        assertEquals(1, repo.ui.value.items.size)
        assertEquals(1, WSNewsRepository(context, client).ui.value.items.size)
    }
}
