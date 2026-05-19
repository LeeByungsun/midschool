package com.bsbarron.midschoolapp.ui.settings

import com.bsbarron.midschoolapp.data.model.SchoolInfo

data class SettingsUiState(
    val schoolQuery: String = "",
    val selectedSchool: SchoolInfo? = null,
    val schoolResults: List<SchoolInfo> = emptyList(),
    val searchMessage: String = "",
    val isSearching: Boolean = false,
    val grade: String = "",
    val classroom: String = "",
    val isRingMode: Boolean = false,
    val notificationEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
