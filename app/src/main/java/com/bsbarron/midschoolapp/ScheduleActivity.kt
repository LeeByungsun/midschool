package com.bsbarron.midschoolapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbarron.midschoolapp.ui.compose.ScreenScaffold
import com.bsbarron.midschoolapp.ui.compose.ScrollColumn
import com.bsbarron.midschoolapp.ui.compose.SectionCard
import com.bsbarron.midschoolapp.ui.schedule.ScheduleUiState
import com.bsbarron.midschoolapp.ui.schedule.ScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduleActivity : AppCompatActivity() {
    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ScheduleScreen(
                uiState = uiState,
                onBack = ::finish,
                onPreviousMonth = viewModel::showPreviousMonth,
                onNextMonth = viewModel::showNextMonth
            )
        }
    }
}

@Composable
private fun ScheduleScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    ScreenScaffold(
        title = androidx.compose.ui.res.stringResource(R.string.home_schedule_title),
        onBack = onBack
    ) {
        ScrollColumn {
            SectionCard(title = uiState.monthTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onPreviousMonth) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.schedule_previous_month))
                    }
                    OutlinedButton(onClick = onNextMonth) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.schedule_next_month))
                    }
                }
                Text(text = uiState.scheduleText)
            }
        }
    }
}
