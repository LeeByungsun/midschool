package com.lbs.schoolhelper

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.view.View
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lbs.schoolhelper.data.model.HomeContentStatus
import com.lbs.schoolhelper.databinding.ActivityMainBinding
import com.lbs.schoolhelper.ui.home.HomeNoticeAction
import com.lbs.schoolhelper.ui.home.HomeViewModel
import com.lbs.schoolhelper.ui.timer.TimerPreset
import com.lbs.schoolhelper.ui.timer.TimerViewModel
import com.lbs.schoolhelper.util.ExternalUrlOpener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.lbs.schoolhelper.util.applySystemBarPadding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var timerCompletionAnimator: ValueAnimator? = null
    private val homeViewModel: HomeViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        binding.main.applySystemBarPadding()

        bindClicks()
        bindHomeState()
        bindHomeEvents()
        bindTimerState()
        maybeRequestNotificationPermission()

        homeViewModel.loadHomeData()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadHomeData()
        timerViewModel.refreshDisplayMode()
    }

    override fun onDestroy() {
        stopTimerCompletionBlink()
        super.onDestroy()
    }

    private fun bindClicks() {
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.mealCard.setOnClickListener {
            startActivity(Intent(this, MealActivity::class.java))
        }
        binding.openNoticeButton.setOnClickListener {
            homeViewModel.onNoticeActionClicked()
        }
        binding.focusPresetCard.setOnClickListener { timerViewModel.selectPreset(TimerPreset.FOCUS) }
        binding.breakPresetCard.setOnClickListener { timerViewModel.selectPreset(TimerPreset.BREAK) }
        binding.deepPresetCard.setOnClickListener { timerViewModel.selectPreset(TimerPreset.DEEP_FOCUS) }
        binding.openTimerDetailButton.setOnClickListener {
            startActivity(Intent(this, TimerActivity::class.java))
        }
        binding.timerPrimaryButton.setOnClickListener { timerViewModel.toggleTimer() }
        binding.timerResetButton.setOnClickListener { timerViewModel.resetTimer() }
    }

    private fun bindHomeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    binding.schoolNameText.text = state.schoolName
                    binding.dateLabelText.text = state.dateLabel
                    binding.classSummaryText.text = state.classSummary
                    binding.todaySummaryText.text = state.todaySummaryText
                    if (!state.isSchoolConfigured) {
                        binding.openTimetableButton.setText(R.string.home_setup_button)
                        binding.openScheduleButton.setText(R.string.home_setup_button)
                        binding.openTimetableButton.setOnClickListener {
                            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                        }
                        binding.openScheduleButton.setOnClickListener {
                            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                        }
                    } else {
                        binding.openTimetableButton.setText(R.string.home_timetable_button)
                        binding.openScheduleButton.setText(R.string.home_schedule_button)
                        binding.openTimetableButton.setOnClickListener {
                            startActivity(Intent(this@MainActivity, TimetableActivity::class.java))
                        }
                        binding.openScheduleButton.setOnClickListener {
                            startActivity(Intent(this@MainActivity, ScheduleActivity::class.java))
                        }
                    }
                    binding.mealMenuText.text = state.mealSummary
                    binding.mealMetaText.text = state.mealMeta
                    binding.mealMetaText.visibility = if (
                        state.mealStatus == HomeContentStatus.SUCCESS ||
                        state.mealStatus == HomeContentStatus.EMPTY
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    binding.scheduleSummaryText.text = state.eventSummary
                    binding.noticeSummaryText.text = state.notices.summary
                    binding.openNoticeButton.text = state.notices.actionText
                    binding.openNoticeButton.isEnabled = state.notices.actionEnabled
                }
            }
        }
    }

    private fun bindHomeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.noticeActionEvent.collect { event ->
                    when (event) {
                        HomeNoticeAction.OpenSetup -> {
                            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                        }

                        is HomeNoticeAction.OpenUrl -> {
                            startActivity(ExternalUrlOpener.buildIntent(event.url))
                        }
                    }
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
                    updateTimerCompletionBlink(state.isCompleted)
                }
            }
        }
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
            duration = 550L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                binding.timerCard.setCardBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun stopTimerCompletionBlink() {
        timerCompletionAnimator?.cancel()
        timerCompletionAnimator = null
        if (::binding.isInitialized) {
            binding.timerCard.setCardBackgroundColor(getColor(R.color.surface_card))
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

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!UserPreferences.isTimerNotificationEnabled(this)) return
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_POST_NOTIFICATIONS
        )
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 4101
    }
}
