package com.lbs.schoolhelper.ui.timetable

import com.lbs.schoolhelper.data.model.TimetableItem

data class TimetableUiState(
    val dateTitle: String = "",
    val classInfoText: String = "",
    val lessonCountText: String = "",
    val statusText: String = "",
    val showTodayButton: Boolean = true,
    val items: List<TimetableItem> = emptyList()
)
