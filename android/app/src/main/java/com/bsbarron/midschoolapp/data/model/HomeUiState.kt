package com.bsbarron.midschoolapp.data.model

data class HomeUiState(
    val schoolName: String = "",
    val dateLabel: String = "",
    val classSummary: String = "",
    val todaySummaryText: String = "",
    val mealSummary: String = "",
    val mealMeta: String = "",
    val eventSummary: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
