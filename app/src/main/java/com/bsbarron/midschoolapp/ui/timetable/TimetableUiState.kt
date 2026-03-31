package com.bsbarron.midschoolapp.ui.timetable

import com.bsbarron.midschoolapp.data.model.TimetableItem

data class TimetableUiState(
    val dateTitle: String = "",
    val classInfoText: String = "",
    val statusText: String = "",
    val items: List<TimetableItem> = emptyList()
)
