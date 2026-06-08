package com.lbs.schoolhelper.timer

import android.app.Application
import com.lbs.schoolhelper.data.repository.TimerPreferenceState
import com.lbs.schoolhelper.test.FakePreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class TimerBootRestorerTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun restore_whenTimerIsRunningInFuture_reschedulesAlarm() {
        val repository = FakePreferencesRepository(
            timerState = TimerPreferenceState(
                presetName = "FOCUS",
                totalMillis = 2_400_000L,
                remainingMillis = 1_800_000L,
                targetAtMillis = 10_000L,
                isRunning = true
            )
        )
        var scheduledAt: Long? = null
        var cancelCalls = 0

        TimerBootRestorer.restore(
            context = application,
            preferencesRepository = repository,
            nowMillis = 5_000L,
            scheduleAlarm = { scheduledAt = it },
            cancelAlarm = { cancelCalls += 1 }
        )

        assertEquals(10_000L, scheduledAt)
        assertEquals(0, cancelCalls)
        assertEquals(0, repository.clearTimerStateCallCount)
    }

    @Test
    fun restore_whenTimerExpired_clearsStateAndCancelsAlarm() {
        val repository = FakePreferencesRepository(
            timerState = TimerPreferenceState(
                presetName = "FOCUS",
                totalMillis = 2_400_000L,
                remainingMillis = 1_000L,
                targetAtMillis = 4_000L,
                isRunning = true
            )
        )
        var scheduledAt: Long? = null
        var cancelCalls = 0

        TimerBootRestorer.restore(
            context = application,
            preferencesRepository = repository,
            nowMillis = 5_000L,
            scheduleAlarm = { scheduledAt = it },
            cancelAlarm = { cancelCalls += 1 }
        )

        assertNull(scheduledAt)
        assertEquals(1, cancelCalls)
        assertEquals(1, repository.clearTimerStateCallCount)
        assertFalse(repository.getTimerState().isRunning)
    }

    @Test
    fun restore_whenTimerNotRunning_doesNothing() {
        val repository = FakePreferencesRepository(
            timerState = TimerPreferenceState(
                presetName = "FOCUS",
                totalMillis = 2_400_000L,
                remainingMillis = 1_000L,
                targetAtMillis = 10_000L,
                isRunning = false
            )
        )
        var scheduledAt: Long? = null
        var cancelCalls = 0

        TimerBootRestorer.restore(
            context = application,
            preferencesRepository = repository,
            nowMillis = 5_000L,
            scheduleAlarm = { scheduledAt = it },
            cancelAlarm = { cancelCalls += 1 }
        )

        assertNull(scheduledAt)
        assertEquals(0, cancelCalls)
        assertEquals(0, repository.clearTimerStateCallCount)
    }
}
