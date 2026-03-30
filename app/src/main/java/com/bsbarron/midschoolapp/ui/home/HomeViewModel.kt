package com.bsbarron.midschoolapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.data.model.HomeUiState
import com.bsbarron.midschoolapp.data.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val mealsResult = schoolRepository.getMeals()
            val schedulesResult = schoolRepository.getSchedules()

            val mealSummary = mealsResult.getOrNull()?.firstOrNull()?.menu.orEmpty()
            val eventSummary = schedulesResult.getOrNull()?.firstOrNull()?.title.orEmpty()
            val errorMessage = mealsResult.exceptionOrNull()?.message
                ?: schedulesResult.exceptionOrNull()?.message

            _uiState.update {
                it.copy(
                    mealSummary = mealSummary,
                    eventSummary = eventSummary,
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }
}
