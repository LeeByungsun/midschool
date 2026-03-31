package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.databinding.ActivityMainBinding
import com.bsbarron.midschoolapp.ui.home.HomeViewModel
import com.bsbarron.midschoolapp.ui.timer.TimerPreset
import com.bsbarron.midschoolapp.ui.timer.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindClicks()
        bindHomeState()
        bindTimerState()

        homeViewModel.loadHomeData()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshHeader()
        timerViewModel.refreshDisplayMode()
    }

    private fun bindClicks() {
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.openTimetableButton.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }
        binding.openScheduleButton.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        binding.focusPresetCard.setOnClickListener { timerViewModel.selectPreset(TimerPreset.FOCUS) }
        binding.breakPresetCard.setOnClickListener { timerViewModel.selectPreset(TimerPreset.BREAK) }
        binding.deepPresetCard.setOnClickListener { timerViewModel.selectPreset(TimerPreset.DEEP_FOCUS) }
        binding.timerPrimaryButton.setOnClickListener { timerViewModel.toggleTimer() }
        binding.timerResetButton.setOnClickListener { timerViewModel.resetTimer() }
    }

    private fun bindHomeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    binding.dateLabelText.text = state.dateLabel
                    binding.classSummaryText.text = state.classSummary
                    binding.todaySummaryText.text = state.todaySummaryText
                    if (state.mealSummary.isNotBlank()) binding.mealMenuText.text = state.mealSummary
                    if (state.mealMeta.isNotBlank()) binding.mealMetaText.text = state.mealMeta
                    if (state.eventSummary.isNotBlank()) binding.scheduleSummaryText.text = state.eventSummary
                }
            }
        }
    }

    private fun bindTimerState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                timerViewModel.uiState.collect { state ->
                    binding.timerCountText.text = state.displayTimeText
                    binding.timerSubtitleText.text = state.subtitle
                    binding.timerPrimaryButton.text = getString(state.buttonTextRes)
                    binding.timerRingView.setTimerState(
                        progressFraction = state.progressFraction,
                        timeText = state.displayTimeText,
                        labelText = getString(R.string.home_timer_remaining)
                    )
                    binding.timerCountText.visibility = if (state.isCountMode) View.VISIBLE else View.GONE
                    binding.timerRingView.visibility = if (state.isCountMode) View.GONE else View.VISIBLE
                    updatePresetSelection(state.selectedPreset)
                }
            }
        }
    }

    private fun updatePresetSelection(selectedPreset: TimerPreset) {
        val selectedColor = getColor(R.color.brand_blue_soft)
        val defaultColor = getColor(R.color.surface_card)
        binding.focusPresetCard.setCardBackgroundColor(
            if (selectedPreset == TimerPreset.FOCUS) selectedColor else defaultColor
        )
        binding.breakPresetCard.setCardBackgroundColor(
            if (selectedPreset == TimerPreset.BREAK) selectedColor else defaultColor
        )
        binding.deepPresetCard.setCardBackgroundColor(
            if (selectedPreset == TimerPreset.DEEP_FOCUS) selectedColor else defaultColor
        )
    }
}
