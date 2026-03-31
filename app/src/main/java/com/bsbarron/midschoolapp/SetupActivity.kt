package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.ui.setup.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup)

        val gradeInput = findViewById<android.widget.EditText>(R.id.gradeInput)
        val classInput = findViewById<android.widget.EditText>(R.id.classInput)
        val saveButton = findViewById<android.widget.Button>(R.id.saveStudentInfoButton)

        saveButton.setOnClickListener {
            viewModel.updateGrade(gradeInput.text.toString().trim())
            viewModel.updateClassroom(classInput.text.toString().trim())
            lifecycleScope.launch {
                viewModel.saveStudentInfo()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }
    }
}
