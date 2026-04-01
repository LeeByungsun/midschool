package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbarron.midschoolapp.ui.compose.PrimaryButton
import com.bsbarron.midschoolapp.ui.compose.ScreenScaffold
import com.bsbarron.midschoolapp.ui.compose.ScrollColumn
import com.bsbarron.midschoolapp.ui.compose.SectionCard
import com.bsbarron.midschoolapp.ui.settings.SettingsUiState
import com.bsbarron.midschoolapp.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
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

            SettingsScreen(
                uiState = uiState,
                onBack = ::finish,
                onGradeChange = viewModel::updateGrade,
                onClassroomChange = viewModel::updateClassroom,
                onRingModeChange = viewModel::updateDisplayMode,
                onNotificationChange = viewModel::updateNotificationEnabled,
                onVibrationChange = viewModel::updateVibrationEnabled,
                onSave = viewModel::saveSettings
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onGradeChange: (String) -> Unit,
    onClassroomChange: (String) -> Unit,
    onRingModeChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onSave: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        title = androidx.compose.ui.res.stringResource(R.string.settings_title),
        onBack = onBack
    ) {
        ScrollColumn {
            SectionCard(
                title = androidx.compose.ui.res.stringResource(R.string.settings_title),
                subtitle = androidx.compose.ui.res.stringResource(R.string.settings_description)
            ) {
                OutlinedTextField(
                    value = uiState.grade,
                    onValueChange = onGradeChange,
                    label = { Text(text = androidx.compose.ui.res.stringResource(R.string.setup_grade_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.classroom,
                    onValueChange = onClassroomChange,
                    label = { Text(text = androidx.compose.ui.res.stringResource(R.string.setup_class_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(
                title = androidx.compose.ui.res.stringResource(R.string.settings_timer_display_title)
            ) {
                SettingRadioRow(
                    label = androidx.compose.ui.res.stringResource(R.string.settings_timer_display_count),
                    selected = !uiState.isRingMode,
                    onClick = { onRingModeChange(false) }
                )
                SettingRadioRow(
                    label = androidx.compose.ui.res.stringResource(R.string.settings_timer_display_ring),
                    selected = uiState.isRingMode,
                    onClick = { onRingModeChange(true) }
                )
            }

            SectionCard(
                title = androidx.compose.ui.res.stringResource(R.string.settings_timer_alert_title)
            ) {
                SettingSwitchRow(
                    label = androidx.compose.ui.res.stringResource(R.string.settings_timer_alert_notification),
                    checked = uiState.notificationEnabled,
                    onCheckedChange = onNotificationChange
                )
                SettingSwitchRow(
                    label = androidx.compose.ui.res.stringResource(R.string.settings_timer_alert_vibration),
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = onVibrationChange
                )
                PrimaryButton(
                    text = androidx.compose.ui.res.stringResource(R.string.settings_save_button),
                    onClick = { scope.launch { onSave() } }
                )
            }
        }
    }
}

@Composable
private fun SettingRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
