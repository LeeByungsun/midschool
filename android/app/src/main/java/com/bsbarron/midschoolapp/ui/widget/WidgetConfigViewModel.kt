package com.bsbarron.midschoolapp.ui.widget

import androidx.lifecycle.ViewModel
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.WidgetSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetConfigUiState())
    val uiState = _uiState.asStateFlow()

    private val _saveEvent = MutableSharedFlow<Unit>()
    val saveEvent = _saveEvent.asSharedFlow()

    fun loadSettings(appWidgetId: Int) {
        val settings = preferencesRepository.getWidgetSettings(appWidgetId)
        _uiState.value = WidgetConfigUiState(
            showTomorrowTimetable = settings.showTomorrowTimetable
        )
    }

    fun updateShowTomorrow(enabled: Boolean) {
        _uiState.update { it.copy(showTomorrowTimetable = enabled) }
    }

    suspend fun saveSettings(appWidgetId: Int) {
        preferencesRepository.saveWidgetSettings(
            appWidgetId = appWidgetId,
            settings = WidgetSettings(
                showTomorrowTimetable = _uiState.value.showTomorrowTimetable
            )
        )
        _saveEvent.emit(Unit)
    }
}
