package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.databinding.ActivitySettingsBinding
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        val rootView = binding.root
        val initialTopPadding = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                initialTopPadding + systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        binding.backButton.setOnClickListener { finish() }

        binding.saveSettingsButton.setOnClickListener {
            // 저장 직전의 화면 값만 ViewModel에 전달해 단일 저장 경로를 유지한다.
            viewModel.updateGrade(binding.settingsGradeInput.text.toString().trim())
            viewModel.updateClassroom(binding.settingsClassInput.text.toString().trim())
            viewModel.updateDisplayMode(binding.timerDisplayRingRadio.isChecked)
            viewModel.updateNotificationEnabled(binding.timerNotificationSwitch.isChecked)
            viewModel.updateVibrationEnabled(binding.timerVibrationSwitch.isChecked)
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
                        if (binding.settingsGradeInput.text.toString() != state.grade) {
                            binding.settingsGradeInput.setText(state.grade)
                        }
                        if (binding.settingsClassInput.text.toString() != state.classroom) {
                            binding.settingsClassInput.setText(state.classroom)
                        }
                        binding.timerDisplayCountRadio.isChecked = !state.isRingMode
                        binding.timerDisplayRingRadio.isChecked = state.isRingMode
                        binding.timerNotificationSwitch.isChecked = state.notificationEnabled
                        binding.timerVibrationSwitch.isChecked = state.vibrationEnabled
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
