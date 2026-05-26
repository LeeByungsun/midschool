package com.bsbarron.midschoolapp.ui.splash

import android.app.Application
import android.content.Context
import android.os.Looper
import com.bsbarron.midschoolapp.data.repository.StudentInfo
import com.bsbarron.midschoolapp.test.FakePreferencesRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SplashViewModelTest {

    private val application = TestApplication()

    @Test
    fun decideNextScreen_whenStudentInfoIsIncomplete_routesToSetup() = runBlocking {
        val viewModel = SplashViewModel(
            application = application,
            preferencesRepository = FakePreferencesRepository(
                studentInfo = StudentInfo(
                    grade = "2",
                    classroom = "5",
                    schoolName = "미사중학교"
                )
            )
        )
        val navigationDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(3_000L) { viewModel.navigationEvent.first() }
        }

        viewModel.decideNextScreen()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_300L))

        assertEquals(SplashDestination.SETUP, navigationDeferred.await())
    }

    @Test
    fun decideNextScreen_whenStudentInfoIsComplete_routesToMain() = runBlocking {
        val viewModel = SplashViewModel(
            application = application,
            preferencesRepository = FakePreferencesRepository(
                studentInfo = StudentInfo(
                    grade = "2",
                    classroom = "5",
                    schoolName = "미사중학교",
                    officeCode = "J10",
                    schoolCode = "1234567",
                    schoolKind = "중학교"
                )
            )
        )
        val navigationDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(3_000L) { viewModel.navigationEvent.first() }
        }

        viewModel.decideNextScreen()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_300L))

        assertEquals(SplashDestination.MAIN, navigationDeferred.await())
    }

    private class TestApplication : Application() {
        override fun getApplicationContext(): Context = this
    }
}
