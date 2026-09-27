package com.lbs.schoolhelper

import android.content.Context
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
class StudentInfoFocusPersistenceTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication().applicationContext

    @Before
    fun setUp() {
        clearPreferences()
    }

    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun editingNameDoesNotLoseFocusedSchoolGradeOrClassDraft() {
        UserPreferences.saveStudentInfo(
            context,
            StudentInfo(
                schoolName = "미사중학교",
                officeCode = "J10",
                schoolCode = "1234567",
                schoolKind = "중학교"
            )
        )
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val nameInput = activity.findViewById<EditText>(R.id.profileNameInput)
        val schoolInput = activity.findViewById<EditText>(R.id.schoolQueryInput)
        val gradeInput = activity.findViewById<EditText>(R.id.gradeInput)
        val classInput = activity.findViewById<EditText>(R.id.classInput)

        assertTrue(nameInput.requestFocus())
        nameInput.setText("민준")
        assertTrue(gradeInput.requestFocus())
        gradeInput.setText("2")
        assertTrue(classInput.requestFocus())
        classInput.setText("3")
        assertTrue(nameInput.requestFocus())

        assertEquals("민준", nameInput.text.toString())
        assertEquals("미사중학교", schoolInput.text.toString())
        assertEquals("2", gradeInput.text.toString())
        assertEquals("3", classInput.text.toString())
    }

    private fun clearPreferences() {
        context.getSharedPreferences("midschool_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("student_profiles", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
