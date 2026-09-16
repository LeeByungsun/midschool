package com.lbs.schoolhelper.ui.settings

import com.lbs.schoolhelper.data.model.SchoolInfo

data class SettingsUiState(
    val schoolQuery: String = "",
    val selectedSchool: SchoolInfo? = null,
    val schoolResults: List<SchoolInfo> = emptyList(),
    val searchMessage: String = "",
    val isSearching: Boolean = false,
    val grade: String = "",
    val classroom: String = "",
    val isRingMode: Boolean = false,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val pomodoroRounds: Int = 4,
    val notificationEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val diagnosticsEnabled: Boolean = false
)
