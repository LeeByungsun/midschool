package com.bsbarron.midschoolapp

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class TimetableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timetable)

        val backButton: ImageButton = findViewById(R.id.timetableBackButton)
        backButton.setOnClickListener { finish() }
    }
}
