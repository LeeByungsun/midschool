package com.bsbarron.midschoolapp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import com.bsbarron.midschoolapp.data.repository.TimerDisplayMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {

    private val studentInfo = preferencesRepository.getStudentInfo()
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            grade = studentInfo.grade,
            classroom = studentInfo.classroom,
            isRingMode = preferencesRepository.getTimerDisplayMode() == TimerDisplayMode.RING,
            notificationEnabled = preferencesRepository.isTimerNotificationEnabled(),
            vibrationEnabled = preferencesRepository.isTimerVibrationEnabled()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<Int>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _closeEvent = MutableSharedFlow<Unit>()
    val closeEvent = _closeEvent.asSharedFlow()

    fun updateGrade(grade: String) {
        _uiState.update { it.copy(grade = grade) }
    }

    fun updateClassroom(classroom: String) {
        _uiState.update { it.copy(classroom = classroom) }
    }

    fun updateDisplayMode(isRingMode: Boolean) {
        _uiState.update { it.copy(isRingMode = isRingMode) }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    suspend fun saveSettings() {
        val state = _uiState.value
        if (state.grade.isBlank() || state.classroom.isBlank()) {
            _messageEvent.emit(R.string.setup_error_empty)
            return
        }

        preferencesRepository.saveStudentInfo(state.grade, state.classroom)
        preferencesRepository.saveTimerDisplayMode(
            if (state.isRingMode) TimerDisplayMode.RING else TimerDisplayMode.COUNT
        )
        preferencesRepository.saveTimerNotificationEnabled(state.notificationEnabled)
        preferencesRepository.saveTimerVibrationEnabled(state.vibrationEnabled)
        _messageEvent.emit(R.string.settings_saved)
        _closeEvent.emit(Unit)
    }
}
