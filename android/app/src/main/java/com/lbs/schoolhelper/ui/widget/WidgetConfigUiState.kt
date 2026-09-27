package com.lbs.schoolhelper.ui.widget

import com.lbs.schoolhelper.data.profile.StudentProfile

data class WidgetConfigUiState(
    val profiles: List<StudentProfile> = emptyList(),
    val selectedProfileId: String = "",
    val showTomorrowTimetable: Boolean = true
)
