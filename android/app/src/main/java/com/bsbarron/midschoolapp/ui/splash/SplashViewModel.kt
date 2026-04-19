package com.bsbarron.midschoolapp.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    application: Application,
    private val preferencesRepository: PreferencesRepository
) : AndroidViewModel(application) {
    private val _navigationEvent = MutableSharedFlow<SplashDestination>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun decideNextScreen() {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MILLIS)
            val destination = if (preferencesRepository.hasStudentInfo()) {
                SplashDestination.MAIN
            } else {
                SplashDestination.SETUP
            }
            _navigationEvent.emit(destination)
        }
    }

    companion object {
        private const val SPLASH_DELAY_MILLIS = 1200L
    }
}
