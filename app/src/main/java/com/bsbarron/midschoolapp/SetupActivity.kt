package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup)

        val gradeInput: EditText = findViewById(R.id.gradeInput)
        val classInput: EditText = findViewById(R.id.classInput)
        val saveButton: Button = findViewById(R.id.saveStudentInfoButton)

        saveButton.setOnClickListener {
            val grade = gradeInput.text.toString().trim()
            val classroom = classInput.text.toString().trim()

            if (grade.isBlank() || classroom.isBlank()) {
                Toast.makeText(this, R.string.setup_error_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserPreferences.saveStudentInfo(this, grade, classroom)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
