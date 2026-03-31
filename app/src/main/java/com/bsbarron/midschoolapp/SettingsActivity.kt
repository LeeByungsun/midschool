package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.Switch
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
        val countModeRadio: RadioButton = findViewById(R.id.timerDisplayCountRadio)
        val ringModeRadio: RadioButton = findViewById(R.id.timerDisplayRingRadio)
        val timerNotificationSwitch: Switch = findViewById(R.id.timerNotificationSwitch)
        val timerVibrationSwitch: Switch = findViewById(R.id.timerVibrationSwitch)
        val saveButton: Button = findViewById(R.id.saveSettingsButton)

        gradeInput.setText(UserPreferences.getGrade(this))
        classInput.setText(UserPreferences.getClassroom(this))
        when (UserPreferences.getTimerDisplayMode(this)) {
            UserPreferences.TIMER_DISPLAY_RING -> ringModeRadio.isChecked = true
            else -> countModeRadio.isChecked = true
        }
        timerNotificationSwitch.isChecked = UserPreferences.isTimerNotificationEnabled(this)
        timerVibrationSwitch.isChecked = UserPreferences.isTimerVibrationEnabled(this)

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            val grade = gradeInput.text.toString().trim()
            val classroom = classInput.text.toString().trim()

            if (grade.isBlank() || classroom.isBlank()) {
                Toast.makeText(this, R.string.setup_error_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserPreferences.saveStudentInfo(this, grade, classroom)
            UserPreferences.saveTimerDisplayMode(
                this,
                if (ringModeRadio.isChecked) {
                    UserPreferences.TIMER_DISPLAY_RING
                } else {
                    UserPreferences.TIMER_DISPLAY_COUNT
                }
            )
            UserPreferences.saveTimerNotificationEnabled(this, timerNotificationSwitch.isChecked)
            UserPreferences.saveTimerVibrationEnabled(this, timerVibrationSwitch.isChecked)
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
