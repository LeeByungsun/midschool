package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.CountDownTimer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.data.AppContainer
import com.bsbarron.midschoolapp.timer.TimerAlarmScheduler
import com.bsbarron.midschoolapp.ui.home.HomeViewModel
import com.bsbarron.midschoolapp.ui.home.HomeViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.bsbarron.midschoolapp.widget.TimerRingView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private var selectedTimerPreset: TimerPreset = TimerPreset.FOCUS
    private var totalTimerMillis: Long = TimerPreset.FOCUS.durationMillis
    private var remainingTimerMillis: Long = TimerPreset.FOCUS.durationMillis
    private var timerCountDown: CountDownTimer? = null
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(AppContainer.schoolRepository)
        )[HomeViewModel::class.java]

        val classSummaryText: TextView = findViewById(R.id.classSummaryText)
        val dateLabelText: TextView = findViewById(R.id.dateLabelText)
        val todaySummaryText: TextView = findViewById(R.id.todaySummaryText)
        val mealMetaText: TextView = findViewById(R.id.mealMetaText)
        val mealMenuText: TextView = findViewById(R.id.mealMenuText)
        val scheduleSummaryText: TextView = findViewById(R.id.scheduleSummaryText)
        val settingsButton: MaterialButton = findViewById(R.id.settingsButton)
        val timetableButton: MaterialButton = findViewById(R.id.openTimetableButton)
        val scheduleButton: MaterialButton = findViewById(R.id.openScheduleButton)
        val timerCountText: TextView = findViewById(R.id.timerCountText)
        val timerSubtitleText: TextView = findViewById(R.id.timerSubtitleText)
        val timerPrimaryButton: MaterialButton = findViewById(R.id.timerPrimaryButton)
        val timerResetButton: MaterialButton = findViewById(R.id.timerResetButton)
        val timerRingView: TimerRingView = findViewById(R.id.timerRingView)
        val focusPresetCard: MaterialCardView = findViewById(R.id.focusPresetCard)
        val breakPresetCard: MaterialCardView = findViewById(R.id.breakPresetCard)
        val deepPresetCard: MaterialCardView = findViewById(R.id.deepPresetCard)

        dateLabelText.text = LocalDate.now().format(
            DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
        )

        classSummaryText.text = getString(
            R.string.home_student_info_format,
            UserPreferences.getGrade(this),
            UserPreferences.getClassroom(this)
        )

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        timetableButton.setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
        }

        scheduleButton.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

        focusPresetCard.setOnClickListener {
            selectTimerPreset(TimerPreset.FOCUS, timerSubtitleText, timerCountText, timerRingView)
            updatePresetSelection(focusPresetCard, breakPresetCard, deepPresetCard)
        }
        breakPresetCard.setOnClickListener {
            selectTimerPreset(TimerPreset.BREAK, timerSubtitleText, timerCountText, timerRingView)
            updatePresetSelection(focusPresetCard, breakPresetCard, deepPresetCard)
        }
        deepPresetCard.setOnClickListener {
            selectTimerPreset(TimerPreset.DEEP_FOCUS, timerSubtitleText, timerCountText, timerRingView)
            updatePresetSelection(focusPresetCard, breakPresetCard, deepPresetCard)
        }

        timerPrimaryButton.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer(timerPrimaryButton)
            } else {
                startTimer(timerPrimaryButton, timerCountText, timerRingView)
            }
        }

        timerResetButton.setOnClickListener {
            resetTimer(timerPrimaryButton, timerCountText, timerRingView)
        }

        updatePresetSelection(focusPresetCard, breakPresetCard, deepPresetCard)
        restoreTimerState(timerSubtitleText, timerPrimaryButton, timerCountText, timerRingView)
        applyTimerDisplayMode(timerCountText, timerRingView)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    if (state.mealSummary.isNotBlank()) {
                        mealMenuText.text = state.mealSummary
                    }

                    if (state.mealMeta.isNotBlank()) {
                        mealMetaText.text = state.mealMeta
                    }

                    if (state.eventSummary.isNotBlank()) {
                        scheduleSummaryText.text = state.eventSummary
                    }

                    if (state.errorMessage != null) {
                        todaySummaryText.text = getString(R.string.home_today_summary_error)
                    }
                }
            }
        }

        homeViewModel.loadHomeData()
    }

    override fun onResume() {
        super.onResume()

        val classSummaryText: TextView = findViewById(R.id.classSummaryText)
        val timerCountText: TextView = findViewById(R.id.timerCountText)
        val timerRingView: TimerRingView = findViewById(R.id.timerRingView)
        val timerSubtitleText: TextView = findViewById(R.id.timerSubtitleText)
        val timerPrimaryButton: MaterialButton = findViewById(R.id.timerPrimaryButton)
        classSummaryText.text = getString(
            R.string.home_student_info_format,
            UserPreferences.getGrade(this),
            UserPreferences.getClassroom(this)
        )
        restoreTimerState(timerSubtitleText, timerPrimaryButton, timerCountText, timerRingView)
        applyTimerDisplayMode(timerCountText, timerRingView)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerCountDown?.cancel()
    }

    private fun selectTimerPreset(
        preset: TimerPreset,
        timerSubtitleText: TextView,
        timerCountText: TextView,
        timerRingView: TimerRingView
    ) {
        selectedTimerPreset = preset
        totalTimerMillis = preset.durationMillis
        remainingTimerMillis = preset.durationMillis
        timerSubtitleText.text = getString(preset.subtitleRes)
        timerCountDown?.cancel()
        TimerAlarmScheduler.cancel(this)
        UserPreferences.saveTimerState(
            this,
            preset.name,
            totalTimerMillis,
            remainingTimerMillis,
            0L,
            false
        )
        isTimerRunning = false
        findViewById<MaterialButton>(R.id.timerPrimaryButton).text = getString(R.string.home_timer_start)
        updateTimerUi(timerCountText, timerRingView)
    }

    private fun startTimer(
        timerPrimaryButton: MaterialButton,
        timerCountText: TextView,
        timerRingView: TimerRingView
    ) {
        timerCountDown?.cancel()
        val targetAtMillis = System.currentTimeMillis() + remainingTimerMillis
        UserPreferences.saveTimerState(
            this,
            selectedTimerPreset.name,
            totalTimerMillis,
            remainingTimerMillis,
            targetAtMillis,
            true
        )
        TimerAlarmScheduler.schedule(this, targetAtMillis)
        timerCountDown = object : CountDownTimer(remainingTimerMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimerMillis = millisUntilFinished
                UserPreferences.saveTimerState(
                    this@MainActivity,
                    selectedTimerPreset.name,
                    totalTimerMillis,
                    remainingTimerMillis,
                    System.currentTimeMillis() + millisUntilFinished,
                    true
                )
                updateTimerUi(timerCountText, timerRingView)
            }

            override fun onFinish() {
                remainingTimerMillis = 0L
                isTimerRunning = false
                TimerAlarmScheduler.cancel(this@MainActivity)
                UserPreferences.clearTimerState(this@MainActivity)
                timerPrimaryButton.text = getString(R.string.home_timer_restart)
                updateTimerUi(timerCountText, timerRingView)
            }
        }.start()
        isTimerRunning = true
        timerPrimaryButton.text = getString(R.string.home_timer_pause)
    }

    private fun pauseTimer(timerPrimaryButton: MaterialButton) {
        timerCountDown?.cancel()
        TimerAlarmScheduler.cancel(this)
        isTimerRunning = false
        UserPreferences.saveTimerState(
            this,
            selectedTimerPreset.name,
            totalTimerMillis,
            remainingTimerMillis,
            0L,
            false
        )
        timerPrimaryButton.text = getString(R.string.home_timer_resume)
    }

    private fun resetTimer(
        timerPrimaryButton: MaterialButton,
        timerCountText: TextView,
        timerRingView: TimerRingView
    ) {
        timerCountDown?.cancel()
        TimerAlarmScheduler.cancel(this)
        isTimerRunning = false
        remainingTimerMillis = totalTimerMillis
        UserPreferences.saveTimerState(
            this,
            selectedTimerPreset.name,
            totalTimerMillis,
            remainingTimerMillis,
            0L,
            false
        )
        timerPrimaryButton.text = getString(R.string.home_timer_start)
        updateTimerUi(timerCountText, timerRingView)
    }

    private fun updateTimerUi(timerCountText: TextView, timerRingView: TimerRingView) {
        val timeText = formatTimerText(remainingTimerMillis)
        timerCountText.text = timeText
        timerRingView.setTimerState(
            progressFraction = if (totalTimerMillis == 0L) 0f else remainingTimerMillis.toFloat() / totalTimerMillis.toFloat(),
            timeText = timeText,
            labelText = getString(R.string.home_timer_remaining)
        )
    }

    private fun applyTimerDisplayMode(timerCountText: TextView, timerRingView: TimerRingView) {
        val displayMode = UserPreferences.getTimerDisplayMode(this)
        if (displayMode == UserPreferences.TIMER_DISPLAY_RING) {
            timerCountText.visibility = View.GONE
            timerRingView.visibility = View.VISIBLE
        } else {
            timerCountText.visibility = View.VISIBLE
            timerRingView.visibility = View.GONE
        }
    }

    private fun updatePresetSelection(
        focusPresetCard: MaterialCardView,
        breakPresetCard: MaterialCardView,
        deepPresetCard: MaterialCardView
    ) {
        val selectedColor = getColor(R.color.brand_blue_soft)
        val defaultColor = getColor(R.color.surface_card)
        focusPresetCard.setCardBackgroundColor(if (selectedTimerPreset == TimerPreset.FOCUS) selectedColor else defaultColor)
        breakPresetCard.setCardBackgroundColor(if (selectedTimerPreset == TimerPreset.BREAK) selectedColor else defaultColor)
        deepPresetCard.setCardBackgroundColor(if (selectedTimerPreset == TimerPreset.DEEP_FOCUS) selectedColor else defaultColor)
    }

    private fun formatTimerText(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.KOREAN, "%02d:%02d", minutes, seconds)
    }

    private fun restoreTimerState(
        timerSubtitleText: TextView,
        timerPrimaryButton: MaterialButton,
        timerCountText: TextView,
        timerRingView: TimerRingView
    ) {
        val savedState = UserPreferences.getTimerState(this)
        selectedTimerPreset = TimerPreset.valueOf(savedState.presetName)
        totalTimerMillis = savedState.totalMillis
        timerSubtitleText.text = getString(selectedTimerPreset.subtitleRes)

        if (savedState.isRunning && savedState.targetAtMillis > 0L) {
            val recalculatedRemaining = (savedState.targetAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingTimerMillis = recalculatedRemaining
            if (recalculatedRemaining > 0L) {
                timerPrimaryButton.text = getString(R.string.home_timer_pause)
                isTimerRunning = false
                startTimer(timerPrimaryButton, timerCountText, timerRingView)
                return
            }

            remainingTimerMillis = 0L
            isTimerRunning = false
            timerPrimaryButton.text = getString(R.string.home_timer_restart)
            UserPreferences.clearTimerState(this)
            TimerAlarmScheduler.cancel(this)
        } else {
            remainingTimerMillis = savedState.remainingMillis.coerceAtLeast(0L)
            isTimerRunning = false
            timerPrimaryButton.text = if (remainingTimerMillis < totalTimerMillis && remainingTimerMillis > 0L) {
                getString(R.string.home_timer_resume)
            } else {
                getString(R.string.home_timer_start)
            }
        }

        updateTimerUi(timerCountText, timerRingView)
    }

    private enum class TimerPreset(val durationMillis: Long, val subtitleRes: Int) {
        FOCUS(40L * 60L * 1000L, R.string.home_timer_focus_label),
        BREAK(10L * 60L * 1000L, R.string.home_timer_break_label),
        DEEP_FOCUS(25L * 60L * 1000L, R.string.home_timer_deep_label)
    }
}
