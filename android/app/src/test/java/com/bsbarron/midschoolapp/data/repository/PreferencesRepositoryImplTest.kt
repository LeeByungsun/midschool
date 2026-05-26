package com.bsbarron.midschoolapp.data.repository

import android.content.Context
import com.bsbarron.midschoolapp.data.model.SchoolEvent
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        clearUserPrefs()
        clearRepositoryPrefs()
        clearUserPrefs()
        preferencesRepository = PreferencesRepositoryImpl(context, Gson(), AndroidUserPreferencesStore(context))
    }

    @After
    fun tearDown() {
        clearUserPrefs()
        clearRepositoryPrefs()
        clearUserPrefs()
    }

    @Test
    fun `student info round trips school identity and completeness`() {
        val studentInfo = StudentInfo(
            grade = "2",
            classroom = "5",
            schoolName = "미사중학교",
            officeCode = "J10",
            schoolCode = "1234567",
            schoolKind = "중학교"
        )

        preferencesRepository.saveStudentInfo(studentInfo)

        assertEquals(studentInfo, preferencesRepository.getStudentInfo())
        assertTrue(preferencesRepository.hasStudentInfo())
    }

    @Test
    fun `has student info returns false when legacy data misses school identity`() {
        userPrefs().edit()
            .putString(KEY_GRADE, "2")
            .putString(KEY_CLASSROOM, "5")
            .putString(KEY_SCHOOL_NAME, "미사중학교")
            .commit()

        assertEquals(
            StudentInfo(
                grade = "2",
                classroom = "5",
                schoolName = "미사중학교"
            ),
            preferencesRepository.getStudentInfo()
        )
        assertFalse(preferencesRepository.hasStudentInfo())
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

    @Test
    fun `timetable cache preserves empty day result for same class and date`() {
        preferencesRepository.saveTimetableCache(
            officeCode = "J10",
            schoolCode = "1234567",
            grade = "3",
            classroom = "2",
            date = "20260519",
            items = emptyList()
        )

        assertEquals(
            emptyList<TimetableItem>(),
            preferencesRepository.getTimetableCache(
                officeCode = "J10",
                schoolCode = "1234567",
                grade = "3",
                classroom = "2",
                date = "20260519"
            )
        )
        assertNull(
            preferencesRepository.getTimetableCache(
                officeCode = "J10",
                schoolCode = "1234567",
                grade = "3",
                classroom = "1",
                date = "20260519"
            )
        )
    }

    private fun repositoryPrefs() =
        context.getSharedPreferences(REPOSITORY_PREFS_NAME, Context.MODE_PRIVATE)

    private fun userPrefs() =
        context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)

    private fun clearRepositoryPrefs() {
        repositoryPrefs().edit().clear().commit()
    }

    private fun clearUserPrefs() {
        userPrefs().edit().clear().commit()
    }

    private fun scheduleCacheKey(officeCode: String, schoolCode: String, date: String): String {
        return "schedule_cache_${officeCode}_${schoolCode}_$date"
    }

    private fun scheduleCacheTimestampKey(officeCode: String, schoolCode: String, date: String): String {
        return "schedule_cache_ts_${officeCode}_${schoolCode}_$date"
    }

    companion object {
        private const val REPOSITORY_PREFS_NAME = "midschool_repository_prefs"
        private const val USER_PREFS_NAME = "midschool_prefs"
        private const val KEY_GRADE = "grade"
        private const val KEY_CLASSROOM = "classroom"
        private const val KEY_SCHOOL_NAME = "school_name"
        private const val SCHEDULE_CACHE_TTL_MILLIS = 12 * 60 * 60 * 1000L
    }
}
