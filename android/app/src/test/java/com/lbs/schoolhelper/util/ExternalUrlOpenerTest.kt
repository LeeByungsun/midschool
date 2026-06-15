package com.lbs.schoolhelper.util

import android.content.Intent
import com.lbs.schoolhelper.SchoolHelperApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class ExternalUrlOpenerTest {
    @Test
    fun buildIntent_createsBrowsableViewIntent() {
        val url = "https://example.com/notices/123"

        val intent = ExternalUrlOpener.buildIntent(url)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(url, intent.dataString)
        assertTrue(intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true)
    }
}
