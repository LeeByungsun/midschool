package com.lbs.schoolhelper

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.databinding.ActivityTimerBinding
import com.lbs.schoolhelper.ui.timer.TimerViewModel
import com.lbs.schoolhelper.util.applySystemBarPadding
import com.lbs.schoolhelper.util.enableSchoolEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerBinding
    private val timerViewModel: TimerViewModel by viewModels()
    private var timerCompletionAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableSchoolEdgeToEdge()
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.root.applySystemBarPadding()

        bindClicks()
        bindTimerState()
    }

    override fun onResume() {
        super.onResume()
        timerViewModel.refreshDisplayMode()
    }

    override fun onDestroy() {
        stopTimerCompletionBlink()
        super.onDestroy()
    }

    private fun bindClicks() {
        binding.timerBackButton.setOnClickListener { finish() }
        binding.focusPresetCard.setOnClickListener { timerViewModel.selectFocusMinutes(25) }
        binding.breakPresetCard.setOnClickListener { timerViewModel.selectFocusMinutes(40) }
        binding.round2Card.setOnClickListener { timerViewModel.selectRounds(2) }
        binding.round4Card.setOnClickListener { timerViewModel.selectRounds(4) }
        binding.timerPrimaryButton.setOnClickListener { timerViewModel.toggleTimer() }
        binding.timerResetButton.setOnClickListener { timerViewModel.resetTimer() }
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
                    updatePresetSelection(state)
                    updateTimerCompletionBlink(state.isCompleted)
                }
            }
        }
    }

    private fun updatePresetSelection(state: com.lbs.schoolhelper.ui.timer.TimerUiState) {
        val selectedColor = getColor(R.color.brand_blue_soft)
        val defaultColor = getColor(R.color.surface_card)
        binding.focusPresetCard.setCardBackgroundColor(
            if (state.focusMinutes == 25) selectedColor else defaultColor
        )
        binding.breakPresetCard.setCardBackgroundColor(
            if (state.focusMinutes == 40) selectedColor else defaultColor
        )
        binding.round2Card.setCardBackgroundColor(if (state.totalRounds == 2) selectedColor else defaultColor)
        binding.round4Card.setCardBackgroundColor(if (state.totalRounds == 4) selectedColor else defaultColor)
    }

    private fun updateTimerCompletionBlink(isCompleted: Boolean) {
        if (!isCompleted) {
            stopTimerCompletionBlink()
            return
        }

        if (timerCompletionAnimator?.isStarted == true) return

        val defaultColor = getColor(R.color.surface_card)
        val alertColor = getColor(R.color.brand_yellow_soft)
        timerCompletionAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            defaultColor,
            alertColor
        ).apply {
            duration = 650L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                binding.timerDetailCard.setCardBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun stopTimerCompletionBlink() {
        timerCompletionAnimator?.cancel()
        timerCompletionAnimator = null
        if (::binding.isInitialized) {
            binding.timerDetailCard.setCardBackgroundColor(getColor(R.color.surface_card))
        }
    }
}
