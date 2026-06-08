package com.lbs.schoolhelper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.lbs.schoolhelper.data.model.HomeNoticeCardState
import com.lbs.schoolhelper.data.model.HomeUiState
import com.lbs.schoolhelper.ui.home.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MisSchoolApplication::class, sdk = [34])
class MainActivityNavigationTest {

    @Test
    fun onCreate_requestsNotificationPermissionWhenTimerAlertsAreEnabled() {
        clearUserPreferences()
        setTimerNotificationEnabled(true)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val permissionRequest = shadowOf(activity).lastRequestedPermission

        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            permissionRequest.requestedPermissions.toList()
        )
    }

    @Test
    fun onCreate_doesNotRequestNotificationPermissionWhenTimerAlertsAreDisabled() {
        clearUserPreferences()
        setTimerNotificationEnabled(false)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        val permissionRequest = shadowOf(activity).lastRequestedPermission

        assertEquals(null, permissionRequest)
    }

    @Test
    fun mealCardClickStartsMealActivity() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.findViewById<View>(R.id.mealCard).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertEquals(MealActivity::class.java.name, nextIntent.component?.className)
    }

    @Test
    fun noticeButtonClickStartsSetupActivityWhenSchoolNotConfigured() {
        clearUserPreferences()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        shadowOf(Looper.getMainLooper()).idle()
        activity.findViewById<View>(R.id.openNoticeButton).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertEquals(SetupActivity::class.java.name, nextIntent.component?.className)
    }

    @Test
    fun noticeButtonClickStartsBrowsableIntentForLatestNotice() {
        clearUserPreferences()
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val viewModel = ViewModelProvider(activity)[HomeViewModel::class.java]
        val noticeUrl = "https://example.com/notices/42"

        homeUiState(viewModel).value = HomeUiState(
            notices = HomeNoticeCardState(
                summary = "수련회 안내",
                actionText = activity.getString(R.string.home_notice_open_button),
                actionEnabled = true,
                latestNoticeUrl = noticeUrl,
                requiresSetup = false
            )
        )

        shadowOf(Looper.getMainLooper()).idle()
        activity.findViewById<View>(R.id.openNoticeButton).performClick()

        val nextIntent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals(noticeUrl, nextIntent.dataString)
        assertTrue(nextIntent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun homeUiState(viewModel: HomeViewModel): MutableStateFlow<HomeUiState> {
        val field = HomeViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        return field.get(viewModel) as MutableStateFlow<HomeUiState>
    }

    private fun clearUserPreferences() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        context.getSharedPreferences("midschool_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun setTimerNotificationEnabled(enabled: Boolean) {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context
        context.getSharedPreferences("midschool_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("timer_notification_enabled", enabled)
            .commit()
    }
}
