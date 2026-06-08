package com.lbs.schoolhelper.ui.common

import android.app.Application
import android.content.res.Resources
import com.lbs.schoolhelper.R
import javax.inject.Inject

interface UiStringProvider {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any?): String
}

class AndroidUiStringProvider @Inject constructor(
    application: Application
) : UiStringProvider {
    private val resources: Resources = application.applicationContext.resources

    override fun getString(resId: Int): String = resources.getString(resId)

    override fun getString(resId: Int, vararg formatArgs: Any?): String = resources.getString(resId, *formatArgs)
}
