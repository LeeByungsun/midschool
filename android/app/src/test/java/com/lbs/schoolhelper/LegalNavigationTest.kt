package com.lbs.schoolhelper

import android.content.Context
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
class LegalNavigationTest {

    @Test
    fun settingsPrivacyLinks_showHelpfulErrorWhenBrowserIsUnavailable() {
        clearProfiles()
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.settingsPrivacyPolicyButton).performClick()

        assertEquals(activity.getString(R.string.legal_link_open_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun setupConsentPrivacyLink_showHelpfulErrorWhenBrowserIsUnavailable() {
        clearProfiles()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.setupPrivacyPolicyButton).performClick()

        assertEquals(activity.getString(R.string.legal_link_open_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun openSourceLicenses_opensGeneratedLicenseScreen() {
        clearProfiles()
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.openSourceLicensesButton).performClick()

        assertEquals(
            "com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity",
            shadowOf(activity).nextStartedActivity.component?.className
        )
    }

    private fun clearProfiles() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        context.getSharedPreferences("student_profiles", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
