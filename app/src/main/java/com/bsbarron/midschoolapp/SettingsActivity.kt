package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<android.widget.ImageButton>(R.id.backButton)
        val gradeInput = findViewById<android.widget.EditText>(R.id.settingsGradeInput)
        val classInput = findViewById<android.widget.EditText>(R.id.settingsClassInput)
        val countModeRadio = findViewById<android.widget.RadioButton>(R.id.timerDisplayCountRadio)
        val ringModeRadio = findViewById<android.widget.RadioButton>(R.id.timerDisplayRingRadio)
        val timerNotificationSwitch = findViewById<android.widget.Switch>(R.id.timerNotificationSwitch)
        val timerVibrationSwitch = findViewById<android.widget.Switch>(R.id.timerVibrationSwitch)
        val saveButton = findViewById<android.widget.Button>(R.id.saveSettingsButton)

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            viewModel.updateGrade(gradeInput.text.toString().trim())
            viewModel.updateClassroom(classInput.text.toString().trim())
            viewModel.updateDisplayMode(ringModeRadio.isChecked)
            viewModel.updateNotificationEnabled(timerNotificationSwitch.isChecked)
            viewModel.updateVibrationEnabled(timerVibrationSwitch.isChecked)
            lifecycleScope.launch {
                viewModel.saveSettings()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (gradeInput.text.toString() != state.grade) {
                            gradeInput.setText(state.grade)
                        }
                        if (classInput.text.toString() != state.classroom) {
                            classInput.setText(state.classroom)
                        }
                        countModeRadio.isChecked = !state.isRingMode
                        ringModeRadio.isChecked = state.isRingMode
                        timerNotificationSwitch.isChecked = state.notificationEnabled
                        timerVibrationSwitch.isChecked = state.vibrationEnabled
                    }
                }
                launch {
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SettingsActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.closeEvent.collect {
                        finish()
                    }
                }
            }
        }
    }
}
