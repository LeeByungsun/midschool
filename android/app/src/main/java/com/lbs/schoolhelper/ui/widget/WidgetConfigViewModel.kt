package com.lbs.schoolhelper.ui.widget

import androidx.lifecycle.ViewModel
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.repository.PreferencesRepository
import com.lbs.schoolhelper.data.repository.WidgetSettings
import com.lbs.schoolhelper.telemetry.AppTelemetry
import com.lbs.schoolhelper.telemetry.NoOpTelemetry
import com.lbs.schoolhelper.telemetry.WidgetAction
import com.lbs.schoolhelper.widget.WidgetProfileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val telemetry: AppTelemetry = NoOpTelemetry
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetConfigUiState())
    val uiState = _uiState.asStateFlow()
    private val _saveEvent = MutableSharedFlow<Unit>()
    val saveEvent = _saveEvent.asSharedFlow()

    fun loadSettings(appWidgetId: Int) {
        val settings = preferencesRepository.getWidgetSettings(appWidgetId)
        val active = studentProfileRepository.activeProfile.value
        val selected = WidgetProfileResolver.resolve(
            settings,
            studentProfileRepository.profiles.value,
            active
        )
        val normalizedSettings = settings.copy(profileId = selected?.id.orEmpty())
        if (normalizedSettings != settings) {
            preferencesRepository.saveWidgetSettings(appWidgetId, normalizedSettings)
        }
        _uiState.value = WidgetConfigUiState(
            profiles = studentProfileRepository.profiles.value,
            selectedProfileId = selected?.id.orEmpty(),
            showTomorrowTimetable = settings.showTomorrowTimetable
        )
    }

    fun updateSelectedProfile(profileId: String) {
        if (studentProfileRepository.getProfile(profileId) == null) return
        _uiState.update { it.copy(selectedProfileId = profileId) }
    }

    fun updateShowTomorrow(enabled: Boolean) {
        _uiState.update { it.copy(showTomorrowTimetable = enabled) }
    }

    suspend fun saveSettings(appWidgetId: Int) {
        val state = _uiState.value
        val selected = studentProfileRepository.getProfile(state.selectedProfileId)
            ?: studentProfileRepository.activeProfile.value
        preferencesRepository.saveWidgetSettings(
            appWidgetId = appWidgetId,
            settings = WidgetSettings(
                profileId = selected?.id.orEmpty(),
                showTomorrowTimetable = state.showTomorrowTimetable
            )
        )
        telemetry.widgetAction(WidgetAction.CONFIGURE)
        _saveEvent.emit(Unit)
    }
}
