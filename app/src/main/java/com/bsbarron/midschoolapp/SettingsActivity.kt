package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val backButton: ImageButton = findViewById(R.id.backButton)
        val gradeInput: EditText = findViewById(R.id.settingsGradeInput)
        val classInput: EditText = findViewById(R.id.settingsClassInput)
        val saveButton: Button = findViewById(R.id.saveSettingsButton)

        gradeInput.setText(UserPreferences.getGrade(this))
        classInput.setText(UserPreferences.getClassroom(this))

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            val grade = gradeInput.text.toString().trim()
            val classroom = classInput.text.toString().trim()

            if (grade.isBlank() || classroom.isBlank()) {
                Toast.makeText(this, R.string.setup_error_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserPreferences.saveStudentInfo(this, grade, classroom)
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
