package com.bsbarron.midschoolapp.ui.settings

data class SettingsUiState(
    val grade: String = "",
    val classroom: String = "",
    val isRingMode: Boolean = false,
    val notificationEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
