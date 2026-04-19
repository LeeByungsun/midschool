package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsbarron.midschoolapp.ui.splash.SplashDestination
import com.bsbarron.midschoolapp.ui.splash.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 스플래시는 목적지만 ViewModel이 결정하고, 실제 화면 전환은 Activity가 맡는다.
                viewModel.navigationEvent.collect { destination ->
                    val nextActivity = when (destination) {
                        SplashDestination.MAIN -> MainActivity::class.java
                        SplashDestination.SETUP -> SetupActivity::class.java
                    }
                    startActivity(Intent(this@SplashActivity, nextActivity))
                    finish()
                }
            }
        }

        // 진입 직후 다음 화면 판단을 시작해 스플래시 노출 시간을 일정하게 유지한다.
        viewModel.decideNextScreen()
    }
}
