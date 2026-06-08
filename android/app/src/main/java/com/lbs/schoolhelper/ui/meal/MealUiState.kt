package com.lbs.schoolhelper.ui.meal

data class MealUiState(
    val weekTitle: String = "",
    val statusText: String = "",
    val isLoading: Boolean = true,
    val items: List<MealDayUiModel> = emptyList()
)

data class MealDayUiModel(
    val dateLabel: String,
    val detailText: String,
    val isToday: Boolean
)
