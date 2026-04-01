package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbarron.midschoolapp.data.model.HomeUiState
import com.bsbarron.midschoolapp.ui.compose.PrimaryButton
import com.bsbarron.midschoolapp.ui.compose.ScreenScaffold
import com.bsbarron.midschoolapp.ui.compose.SectionCard
import com.bsbarron.midschoolapp.ui.compose.TimerRing
import com.bsbarron.midschoolapp.ui.home.HomeViewModel
import com.bsbarron.midschoolapp.ui.timer.TimerPreset
import com.bsbarron.midschoolapp.ui.timer.TimerUiState
import com.bsbarron.midschoolapp.ui.timer.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val timerState by timerViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                homeViewModel.loadHomeData()
            }

            MainScreen(
                homeState = homeState,
                timerState = timerState,
                onSettingsClick = {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                onOpenTimetable = {
                    startActivity(Intent(this, TimetableActivity::class.java))
                },
                onOpenSchedule = {
                    startActivity(Intent(this, ScheduleActivity::class.java))
                },
                onPresetSelected = timerViewModel::selectPreset,
                onTimerPrimaryClick = timerViewModel::toggleTimer,
                onTimerResetClick = timerViewModel::resetTimer
            )
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshHeader()
        timerViewModel.refreshDisplayMode()
    }
}

@Composable
private fun MainScreen(
    homeState: HomeUiState,
    timerState: TimerUiState,
    onSettingsClick: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenSchedule: () -> Unit,
    onPresetSelected: (TimerPreset) -> Unit,
    onTimerPrimaryClick: () -> Unit,
    onTimerResetClick: () -> Unit
) {
    ScreenScaffold(
        title = stringResourceSafe(R.string.app_name),
        actions = {
            OutlinedButton(onClick = onSettingsClick) {
                Text(text = stringResourceSafe(R.string.home_settings_button))
            }
        }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SectionCard(
                    title = stringResourceSafe(R.string.home_school_name),
                    subtitle = homeState.dateLabel
                ) {
                    Text(
                        text = homeState.classSummary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = homeState.todaySummaryText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SectionCard(
                    title = stringResourceSafe(R.string.home_meal_title),
                    subtitle = homeState.mealMeta
                ) {
                    Text(text = homeState.mealSummary)
                }
            }
            item {
                SectionCard(
                    title = stringResourceSafe(R.string.home_schedule_title),
                    subtitle = null
                ) {
                    Text(text = homeState.eventSummary)
                    PrimaryButton(
                        text = stringResourceSafe(R.string.home_schedule_button),
                        onClick = onOpenSchedule
                    )
                }
            }
            item {
                SectionCard(
                    title = stringResourceSafe(R.string.home_timetable_title),
                    subtitle = null
                ) {
                    Text(
                        text = homeState.classSummary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton(
                        text = stringResourceSafe(R.string.home_timetable_button),
                        onClick = onOpenTimetable
                    )
                }
            }
            item {
                TimerSection(
                    state = timerState,
                    onPresetSelected = onPresetSelected,
                    onPrimaryClick = onTimerPrimaryClick,
                    onResetClick = onTimerResetClick
                )
            }
        }
    }
}

@Composable
private fun TimerSection(
    state: TimerUiState,
    onPresetSelected: (TimerPreset) -> Unit,
    onPrimaryClick: () -> Unit,
    onResetClick: () -> Unit
) {
    SectionCard(
        title = stringResourceSafe(R.string.home_timer_title),
        subtitle = state.subtitle
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerPreset.entries.forEach { preset ->
                val isSelected = state.selectedPreset == preset
                AssistChip(
                    onClick = { onPresetSelected(preset) },
                    label = { Text(text = stringResourceSafe(preset.subtitleRes)) },
                    modifier = Modifier.padding(end = 4.dp)
                )
                if (isSelected) {
                    Text(
                        text = "",
                        modifier = Modifier.width(0.dp)
                    )
                }
            }
        }

        if (state.isCountMode) {
            Text(
                text = state.displayTimeText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TimerRing(
                progressFraction = state.progressFraction,
                timeText = state.displayTimeText,
                labelText = stringResourceSafe(R.string.home_timer_remaining),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResourceSafe(state.buttonTextRes),
                onClick = onPrimaryClick,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onResetClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(text = stringResourceSafe(R.string.home_timer_reset))
            }
        }
    }
}

@Composable
private fun stringResourceSafe(resId: Int): String = androidx.compose.ui.res.stringResource(id = resId)
