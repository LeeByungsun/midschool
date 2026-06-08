package com.lbs.schoolhelper.test

import android.app.Application
import android.content.Context

class TestApplication : Application() {
    override fun getApplicationContext(): Context = this
}
