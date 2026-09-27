package com.lbs.schoolhelper

import android.content.Context
import android.view.WindowManager
import android.widget.EditText
import com.lbs.schoolhelper.data.repository.StudentInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SchoolHelperApplication::class, sdk = [34])
@Suppress("DEPRECATION")
class StudentInfoFocusPersistenceTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication().applicationContext

    private val selectedSchool = StudentInfo(
        schoolName = "미사중학교",
        officeCode = "J10",
        schoolCode = "1234567",
        schoolKind = "중학교"
    )

    @Before
    fun setUp() {
        clearPreferences()
    }

    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun setupInputLosingFocus_persistsValueWithoutSaveButton() {
        UserPreferences.saveStudentInfo(context, selectedSchool)
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val gradeInput = activity.findViewById<EditText>(R.id.gradeInput)
        val classInput = activity.findViewById<EditText>(R.id.classInput)

        assertTrue(gradeInput.requestFocus())
        gradeInput.setText("2")
        assertTrue(classInput.requestFocus())

        assertEquals("2", UserPreferences.getStudentInfo(context).grade)
        assertEquals(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            activity.window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        )
    }

    @Test
    fun settingsInputLosingFocus_persistsValueWithoutConfirmingSettings() {
        UserPreferences.saveStudentInfo(
            context,
            selectedSchool.copy(grade = "1", classroom = "2")
        )
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val gradeInput = activity.findViewById<EditText>(R.id.settingsGradeInput)
        val classInput = activity.findViewById<EditText>(R.id.settingsClassInput)

        assertTrue(classInput.requestFocus())
        classInput.setText("7")
        assertTrue(gradeInput.requestFocus())

        assertEquals("7", UserPreferences.getStudentInfo(context).classroom)
        assertEquals(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            activity.window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        )
    }

    private fun clearPreferences() {
        context.getSharedPreferences("midschool_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
