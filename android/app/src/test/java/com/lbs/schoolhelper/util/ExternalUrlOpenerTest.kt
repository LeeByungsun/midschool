package com.lbs.schoolhelper.util

import android.content.Intent
import com.lbs.schoolhelper.SchoolHelperApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class ExternalUrlOpenerTest {
    @Test
    fun buildIntent_createsBrowsableViewIntent_forHttpsUrl() {
        val url = "https://example.com/notices/123"

        val intent = ExternalUrlOpener.buildIntent(url)

        requireNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(url, intent.dataString)
        assertTrue(intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true)
    }

    @Test
    fun buildIntent_rejectsNonHttpsOrMalformedUrls() {
        listOf(
            "http://example.com/notices",
            "intent://example.com/#Intent;scheme=https;end",
            "javascript:alert(1)",
            "https:///missing-host",
            "not a url"
        ).forEach { url ->
            assertNull("Expected unsafe URL to be rejected: $url", ExternalUrlOpener.buildIntent(url))
        }
    }
}
