package com.lbs.schoolhelper.ui.settings

import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.profile.StudentProfile

data class SettingsUiState(
    val profiles: List<StudentProfile> = emptyList(),
    val editingProfileId: String? = null,
    val displayName: String = "",
    val canDeleteProfile: Boolean = false,
    val hasUnsavedProfileChanges: Boolean = false,
    val schoolQuery: String = "",
    val selectedSchool: SchoolInfo? = null,
    val schoolResults: List<SchoolInfo> = emptyList(),
    val searchMessage: String = "",
    val isSearching: Boolean = false,
    val grade: String = "",
    val classroom: String = "",
    val isRingMode: Boolean = false,
    val notificationEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val diagnosticsEnabled: Boolean = false
)
