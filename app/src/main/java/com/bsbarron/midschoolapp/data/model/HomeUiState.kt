package com.bsbarron.midschoolapp.data.model

data class HomeUiState(
    val mealSummary: String = "",
    val eventSummary: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
