package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import com.bsbarron.midschoolapp.ui.setup.SetupUiState
import com.bsbarron.midschoolapp.ui.setup.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                launch {
                    viewModel.messageEvent.collect { messageRes ->
                        Toast.makeText(this@SetupActivity, messageRes, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.navigationEvent.collect {
                        startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }

            SetupScreen(
                uiState = uiState,
                onGradeChange = viewModel::updateGrade,
                onClassroomChange = viewModel::updateClassroom,
                onSaveClick = viewModel::saveStudentInfo
            )
        }
    }
}

@Composable
private fun SetupScreen(
    uiState: SetupUiState,
    onGradeChange: (String) -> Unit,
    onClassroomChange: (String) -> Unit,
    onSaveClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()

    ScreenScaffold(title = androidx.compose.ui.res.stringResource(R.string.setup_title)) {
        ScrollColumn {
            SectionCard(
                title = androidx.compose.ui.res.stringResource(R.string.setup_title),
                subtitle = androidx.compose.ui.res.stringResource(R.string.setup_description)
            ) {
                OutlinedTextField(
                    value = uiState.grade,
                    onValueChange = onGradeChange,
                    label = { Text(text = androidx.compose.ui.res.stringResource(R.string.setup_grade_label)) },
                    placeholder = { Text(text = androidx.compose.ui.res.stringResource(R.string.setup_grade_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.classroom,
                    onValueChange = onClassroomChange,
                    label = { Text(text = androidx.compose.ui.res.stringResource(R.string.setup_class_label)) },
                    placeholder = { Text(text = androidx.compose.ui.res.stringResource(R.string.setup_class_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = androidx.compose.ui.res.stringResource(R.string.setup_save_button),
                    onClick = { scope.launch { onSaveClick() } }
                )
            }
        }
    }
}
