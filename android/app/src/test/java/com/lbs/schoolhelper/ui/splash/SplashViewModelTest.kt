package com.lbs.schoolhelper.ui.splash

import android.app.Application
import android.content.Context
import android.os.Looper
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeStudentProfileRepository
import java.time.Duration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SplashViewModelTest {

    private val application = TestApplication()

    @Test
    fun splash_withReadyProfileAndConsent_navigatesMain() = runBlocking {
        assertEquals(
            SplashDestination.MAIN,
            navigate(viewModel(profile = readyProfile(), consentCompleted = true))
        )
    }

    @Test
    fun splash_withUnnamedMigratedProfile_navigatesSetup() = runBlocking {
        assertEquals(
            SplashDestination.SETUP,
            navigate(
                viewModel(
                    profile = readyProfile().copy(displayName = ""),
                    consentCompleted = true
                )
            )
        )
    }

    @Test
    fun decideNextScreen_whenProfileIsIncomplete_routesToSetup() = runBlocking {
        val profile = readyProfile().let {
            it.copy(studentInfo = it.studentInfo.copy(schoolCode = ""))
        }

        assertEquals(
            SplashDestination.SETUP,
            navigate(viewModel(profile = profile, consentCompleted = true))
        )
    }

    @Test
    fun decideNextScreen_whenReadyProfileHasNotAnsweredConsent_routesToSetup() = runBlocking {
        assertEquals(
            SplashDestination.SETUP,
            navigate(viewModel(profile = readyProfile(), consentCompleted = false))
        )
    }

    @Test
    fun decideNextScreen_whenCalledTwice_keepsResolvedDestination() = runBlocking {
        val viewModel = viewModel(profile = readyProfile(), consentCompleted = true)
        val firstNavigation = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(3_000L) { viewModel.navigationEvent.first() }
        }

        viewModel.decideNextScreen()
        viewModel.decideNextScreen()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_300L))

        assertEquals(SplashDestination.MAIN, firstNavigation.await())
    }

    @Test
    fun decideNextScreen_whenCollectorStartsAfterDecision_replaysDestination() = runBlocking {
        val viewModel = viewModel(profile = readyProfile(), consentCompleted = true)

        viewModel.decideNextScreen()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_300L))

        val navigation = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(200L) { viewModel.navigationEvent.first() }
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250L))

        assertEquals(SplashDestination.MAIN, navigation.await())
    }

    private fun viewModel(
        profile: StudentProfile?,
        consentCompleted: Boolean
    ): SplashViewModel {
        val profiles = listOfNotNull(profile)
        return SplashViewModel(
            application = application,
            preferencesRepository = FakePreferencesRepository(
                telemetryConsentPromptCompleted = consentCompleted
            ),
            studentProfileRepository = FakeStudentProfileRepository(
                initialProfiles = profiles,
                initialActiveProfileId = profile?.id
            )
        )
    }

    private suspend fun navigate(viewModel: SplashViewModel): SplashDestination {
        val navigationDeferred = kotlinx.coroutines.coroutineScope {
            async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(3_000L) { viewModel.navigationEvent.first() }
            }.also {
                viewModel.decideNextScreen()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_300L))
            }
        }
        return navigationDeferred.await()
    }

    private fun readyProfile(): StudentProfile {
        return StudentProfile(
            id = "profile-1",
            displayName = "민준",
            studentInfo = StudentInfo(
                grade = "2",
                classroom = "5",
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
    }

    private class TestApplication : Application() {
        override fun getApplicationContext(): Context = this
    }
}
