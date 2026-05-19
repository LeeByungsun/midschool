package com.bsbarron.midschoolapp.ui.setup

import com.bsbarron.midschoolapp.data.model.SchoolInfo

data class SetupUiState(
    val schoolQuery: String = "",
    val selectedSchool: SchoolInfo? = null,
    val schoolResults: List<SchoolInfo> = emptyList(),
    val searchMessage: String = "",
    val isSearching: Boolean = false,
    val grade: String = "",
    val classroom: String = ""
)
