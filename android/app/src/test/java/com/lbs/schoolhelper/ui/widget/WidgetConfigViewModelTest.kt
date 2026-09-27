package com.lbs.schoolhelper.ui.widget

import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.test.FakePreferencesRepository
import com.lbs.schoolhelper.test.FakeStudentProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetConfigViewModelTest {
    @Test
    fun config_loadsSavedWidgetProfile() {
        val first = profile("first", "민준")
        val second = profile("second", "서연")
        val preferences = FakePreferencesRepository().apply {
            saveWidgetSettings(42, com.lbs.schoolhelper.data.repository.WidgetSettings(second.id, false))
        }
        val viewModel = WidgetConfigViewModel(
            preferences,
            FakeStudentProfileRepository(listOf(first, second), first.id)
        )

        viewModel.loadSettings(42)

        assertEquals(second.id, viewModel.uiState.value.selectedProfileId)
        assertEquals(false, viewModel.uiState.value.showTomorrowTimetable)
    }

    @Test
    fun config_newWidgetDefaultsToActiveProfile() {
        val first = profile("first", "민준")
        val viewModel = WidgetConfigViewModel(
            FakePreferencesRepository(),
            FakeStudentProfileRepository(listOf(first), first.id)
        )

        viewModel.loadSettings(42)

        assertEquals(first.id, viewModel.uiState.value.selectedProfileId)
    }

    @Test
    fun config_savePersistsProfileIdAndTomorrowFlag() = runBlocking {
        val first = profile("first", "민준")
        val preferences = FakePreferencesRepository()
        val viewModel = WidgetConfigViewModel(
            preferences,
            FakeStudentProfileRepository(listOf(first), first.id)
        )
        viewModel.loadSettings(42)
        viewModel.updateShowTomorrow(false)
        viewModel.saveSettings(42)

        assertEquals(first.id, preferences.getWidgetSettings(42).profileId)
        assertEquals(false, preferences.getWidgetSettings(42).showTomorrowTimetable)
    }

    private fun profile(id: String, name: String) = StudentProfile(
        id,
        name,
        StudentInfo("2", "3", "미사중학교", "J10", id.filter { it.isDigit() }.ifBlank { "1234567" }, "중학교")
    )
}
