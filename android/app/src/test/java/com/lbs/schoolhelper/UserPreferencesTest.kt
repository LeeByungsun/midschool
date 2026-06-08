package com.lbs.schoolhelper

import android.content.Context
import com.lbs.schoolhelper.data.repository.StudentInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class UserPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun saveStudentInfo_roundTripsSchoolIdentityAndMarksSetupComplete() {
        val studentInfo = StudentInfo(
            grade = "2",
            classroom = "3",
            schoolName = "미사중학교",
            officeCode = "J10",
            schoolCode = "1234567",
            schoolKind = "중학교"
        )

        UserPreferences.saveStudentInfo(context, studentInfo)

        assertEquals(studentInfo, UserPreferences.getStudentInfo(context))
        assertTrue(UserPreferences.hasStudentInfo(context))
    }

    @Test
    fun hasStudentInfo_whenLegacyPrefsMissSchoolIdentity_returnsFalse() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GRADE, "2")
            .putString(KEY_CLASSROOM, "3")
            .putString(KEY_SCHOOL_NAME, "미사중학교")
            .commit()

        val stored = UserPreferences.getStudentInfo(context)

        assertEquals("2", stored.grade)
        assertEquals("3", stored.classroom)
        assertEquals("미사중학교", stored.schoolName)
        assertEquals("", stored.officeCode)
        assertEquals("", stored.schoolCode)
        assertEquals("", stored.schoolKind)
        assertFalse(UserPreferences.hasStudentInfo(context))
    }

    private fun clearPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    companion object {
        private const val PREFS_NAME = "midschool_prefs"
        private const val KEY_GRADE = "grade"
        private const val KEY_CLASSROOM = "classroom"
        private const val KEY_SCHOOL_NAME = "school_name"
    }
}
