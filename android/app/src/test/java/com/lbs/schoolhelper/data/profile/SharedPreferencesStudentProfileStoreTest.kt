package com.lbs.schoolhelper.data.profile

import android.content.Context
import com.google.gson.Gson
import com.lbs.schoolhelper.data.repository.StudentInfo
import org.junit.After
import org.junit.Assert.assertEquals
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
class SharedPreferencesStudentProfileStoreTest {

    private lateinit var context: Context
    private lateinit var store: SharedPreferencesStudentProfileStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        clearPreferences()
        store = SharedPreferencesStudentProfileStore(context, Gson())
    }

    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun writeAndRead_roundTripsProfilesAndActiveId() {
        val first = profile(
            id = "profile-1",
            displayName = "민준",
            schoolName = "한빛중학교",
            schoolCode = "7010000",
            grade = "2",
            classroom = "3"
        )
        val second = profile(
            id = "profile-2",
            displayName = "서연",
            schoolName = "한빛초등학교",
            schoolCode = "7020000",
            grade = "5",
            classroom = "1"
        )
        val expected = StudentProfilesData(
            activeProfileId = second.id,
            profiles = listOf(first, second)
        )

        assertTrue(store.write(expected))

        assertEquals(expected, store.read())
    }

    @Test
    fun read_whenJsonIsMalformed_returnsNull() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PROFILES_DATA_KEY, "{not-valid-json")
            .commit()

        assertNull(store.read())
    }

    @Test
    fun readResult_whenJsonIsMalformed_reportsCorruptDocumentAndPreservesRawJson() {
        val rawJson = "{not-valid-json"
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PROFILES_DATA_KEY, rawJson)
            .commit()

        val result = store.readResult()

        assertTrue(result is StudentProfileReadResult.Corrupt)
        assertEquals(rawJson, (result as StudentProfileReadResult.Corrupt).rawJson)
    }

    @Test
    fun readResult_whenJsonIsMissing_reportsMissingDocument() {
        assertEquals(StudentProfileReadResult.Missing, store.readResult())
    }

    @Test
    fun read_restoresCanonicalKeysFromPreR8ProfileJson() {
        val json = """
            {"schemaVersion":1,"activeProfileId":"profile-1","profiles":[
              {"id":"profile-1","displayName":"민준","studentInfo":{
                "grade":"2","classroom":"3","schoolName":"한빛중학교",
                "officeCode":"B10","schoolCode":"7010000","schoolKind":"중학교"
              }}]
            }
        """.trimIndent()
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PROFILES_DATA_KEY, json)
            .commit()

        val profile = requireNotNull(store.read()).profiles.single()

        assertEquals("2", profile.studentInfo.grade)
        assertEquals("3", profile.studentInfo.classroom)
        assertEquals("한빛중학교", profile.studentInfo.schoolName)
        assertEquals("B10", profile.studentInfo.officeCode)
        assertEquals("7010000", profile.studentInfo.schoolCode)
        assertEquals("중학교", profile.studentInfo.schoolKind)
    }

    @Test
    fun read_whenFileDoesNotExist_returnsNull() {
        assertNull(store.read())
    }

    @Test
    fun write_replacesTheWholeDocumentWithoutOrphanedProfiles() {
        val first = profile(id = "profile-1", displayName = "민준")
        val second = profile(id = "profile-2", displayName = "서연")
        assertTrue(
            store.write(
                StudentProfilesData(
                    activeProfileId = first.id,
                    profiles = listOf(first, second)
                )
            )
        )

        val replacement = StudentProfilesData(
            activeProfileId = second.id,
            profiles = listOf(second)
        )
        assertTrue(store.write(replacement))

        assertEquals(replacement, store.read())
    }

    private fun profile(
        id: String,
        displayName: String,
        schoolName: String = "테스트중학교",
        schoolCode: String = "7000000",
        grade: String = "1",
        classroom: String = "2"
    ): StudentProfile {
        return StudentProfile(
            id = id,
            displayName = displayName,
            studentInfo = StudentInfo(
                grade = grade,
                classroom = classroom,
                schoolName = schoolName,
                officeCode = "B10",
                schoolCode = schoolCode,
                schoolKind = if (schoolName.endsWith("초등학교")) "초등학교" else "중학교"
            )
        )
    }

    private fun clearPreferences() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    companion object {
        private const val PREFERENCES_NAME = "student_profiles"
        private const val PROFILES_DATA_KEY = "profiles_data"
    }
}
