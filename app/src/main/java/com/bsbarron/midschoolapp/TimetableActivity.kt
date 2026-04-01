package com.bsbarron.midschoolapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.ui.compose.ScreenScaffold
import com.bsbarron.midschoolapp.ui.compose.ScrollColumn
import com.bsbarron.midschoolapp.ui.compose.SectionCard
import com.bsbarron.midschoolapp.ui.timetable.TimetableUiState
import com.bsbarron.midschoolapp.ui.timetable.TimetableViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimetableActivity : AppCompatActivity() {
    private val viewModel: TimetableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            TimetableScreen(
                uiState = uiState,
                onBack = ::finish,
                onPreviousDay = viewModel::showPreviousDay,
                onToday = viewModel::showToday,
                onNextDay = viewModel::showNextDay
            )
        }
    }
}

@Composable
private fun TimetableScreen(
    uiState: TimetableUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit
) {
    ScreenScaffold(
        title = androidx.compose.ui.res.stringResource(R.string.timetable_title),
        onBack = onBack
    ) {
        ScrollColumn {
            SectionCard(title = uiState.dateTitle, subtitle = uiState.classInfoText) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onPreviousDay) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.timetable_previous_day))
                    }
                    OutlinedButton(onClick = onToday) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.timetable_today))
                    }
                    OutlinedButton(onClick = onNextDay) {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.timetable_next_day))
                    }
                }

                if (uiState.statusText.isNotBlank()) {
                    Text(text = uiState.statusText)
                }

                uiState.items.forEach { item ->
                    TimetableRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun TimetableRow(item: TimetableItem) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.timetable_period_format, item.period),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = item.subject.ifBlank {
                    androidx.compose.ui.res.stringResource(R.string.timetable_no_subject)
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
