package com.bsbarron.midschoolapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bsbarron.midschoolapp.ui.compose.AppSurface
import com.bsbarron.midschoolapp.ui.splash.SplashDestination
import com.bsbarron.midschoolapp.ui.splash.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                launchNavigation()
                viewModel.decideNextScreen()
            }
            SplashScreen()
        }
    }

    private suspend fun launchNavigation() {
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

@androidx.compose.runtime.Composable
private fun SplashScreen() {
    AppSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
