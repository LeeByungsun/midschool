package com.bsbarron.midschoolapp.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {
    private val _navigationEvent = MutableSharedFlow<SplashDestination>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    private var decideNextScreenJob: Job? = null
    private var navigationDispatched = false

    fun decideNextScreen() {
        if (navigationDispatched || decideNextScreenJob?.isActive == true) return

        decideNextScreenJob = viewModelScope.launch {
            delay(SPLASH_DELAY_MILLIS)
            val destination = if (preferencesRepository.hasStudentInfo()) {
                SplashDestination.MAIN
            } else {
                SplashDestination.SETUP
            }
            navigationDispatched = true
            _navigationEvent.emit(destination)
        }
    }

    companion object {
        private const val SPLASH_DELAY_MILLIS = 1200L
    }
}
