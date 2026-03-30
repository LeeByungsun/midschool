package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val nextActivity = if (UserPreferences.hasStudentInfo(this)) {
                MainActivity::class.java
            } else {
                SetupActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            finish()
        }, SPLASH_DELAY_MILLIS)
    }

    companion object {
        private const val SPLASH_DELAY_MILLIS = 1200L
    }
}
