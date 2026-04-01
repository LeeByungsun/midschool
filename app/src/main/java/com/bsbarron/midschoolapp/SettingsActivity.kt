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

        // 설정 화면은 입력값이 많아서 항목별 위젯을 분명하게 나눠 잡아둔다.
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
            // 저장 직전의 화면 값만 ViewModel에 전달해 단일 저장 경로를 유지한다.
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
                    // 이미 반영된 텍스트를 다시 setText 하지 않도록 비교해
                    // 커서 위치가 불필요하게 흔들리는 문제를 막는다.
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
                    // 저장 결과 메시지는 상태와 분리된 이벤트로 처리한다.
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SettingsActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    // 저장 완료 후 닫기까지 ViewModel 이벤트를 통해 일관되게 제어한다.
                    viewModel.closeEvent.collect {
                        finish()
                    }
                }
            }
        }
    }
}
