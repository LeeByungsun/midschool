package com.bsbarron.midschoolapp.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bsbarron.midschoolapp.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerBootReceiver : BroadcastReceiver() {
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        TimerBootRestorer.restore(
            context = context,
            preferencesRepository = preferencesRepository
        )
    }
}
