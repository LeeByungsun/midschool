package com.bsbarron.midschoolapp.data.repository

import android.content.Context
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PreferencesRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        clearRepositoryPrefs()
        preferencesRepository = PreferencesRepositoryImpl(context, Gson())
    }

    @After
    fun tearDown() {
        clearRepositoryPrefs()
    }

    @Test
    fun `schedule cache preserves empty month result for same school and month`() {
        preferencesRepository.saveScheduleCache(
            officeCode = "J10",
            schoolCode = "1234567",
            date = "202605",
            events = emptyList()
        )

        assertEquals(
            emptyList<SchoolEvent>(),
            preferencesRepository.getScheduleCache(
                officeCode = "J10",
                schoolCode = "1234567",
                date = "202605"
            )
        )
        assertNull(
            preferencesRepository.getScheduleCache(
                officeCode = "J10",
                schoolCode = "7654321",
                date = "202605"
            )
        )
    }

    @Test
    fun `schedule cache expires after twelve hours and clears stale entries`() {
        val officeCode = "J10"
        val schoolCode = "1234567"
        val month = "202605"
        preferencesRepository.saveScheduleCache(
            officeCode = officeCode,
            schoolCode = schoolCode,
            date = month,
            events = listOf(
                SchoolEvent(
                    date = "20260519",
                    title = "체육대회",
                    description = "운동장"
                )
            )
        )
        repositoryPrefs().edit()
            .putLong(
                scheduleCacheTimestampKey(officeCode, schoolCode, month),
                System.currentTimeMillis() - SCHEDULE_CACHE_TTL_MILLIS - 1L
            )
            .commit()

        assertNull(
            preferencesRepository.getScheduleCache(
                officeCode = officeCode,
                schoolCode = schoolCode,
                date = month
            )
        )
        assertFalse(repositoryPrefs().contains(scheduleCacheKey(officeCode, schoolCode, month)))
        assertFalse(repositoryPrefs().contains(scheduleCacheTimestampKey(officeCode, schoolCode, month)))
    }

    private fun repositoryPrefs() =
        context.getSharedPreferences(REPOSITORY_PREFS_NAME, Context.MODE_PRIVATE)

    private fun clearRepositoryPrefs() {
        repositoryPrefs().edit().clear().commit()
    }

    private fun scheduleCacheKey(officeCode: String, schoolCode: String, date: String): String {
        return "schedule_cache_${officeCode}_${schoolCode}_$date"
    }

    private fun scheduleCacheTimestampKey(officeCode: String, schoolCode: String, date: String): String {
        return "schedule_cache_ts_${officeCode}_${schoolCode}_$date"
    }

    companion object {
        private const val REPOSITORY_PREFS_NAME = "midschool_repository_prefs"
        private const val SCHEDULE_CACHE_TTL_MILLIS = 12 * 60 * 60 * 1000L
    }
}
