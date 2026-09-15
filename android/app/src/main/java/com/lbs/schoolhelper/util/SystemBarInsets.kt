package com.lbs.schoolhelper.util

import android.graphics.Color
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Applies system bar insets while preserving XML padding.
 *
 * Edge-to-edge screens need bottom inset padding so the last scroll/list item can
 * rest above the soft navigation keys instead of being hidden underneath them.
 */
fun View.applySystemBarPadding(
    applyLeft: Boolean = true,
    applyTop: Boolean = true,
    applyRight: Boolean = true,
    applyBottom: Boolean = true
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            initialLeft + if (applyLeft) systemBars.left else 0,
            initialTop + if (applyTop) systemBars.top else 0,
            initialRight + if (applyRight) systemBars.right else 0,
            initialBottom + if (applyBottom) systemBars.bottom else 0
        )
        insets
    }
}

/**
 * Configures edge-to-edge system bars for the app's light surfaces.
 *
 * Most screens use a light page background, so the status-bar content must use
 * dark icons. The splash screen opts into light icons because its surface is
 * the navy brand color.
 */
fun AppCompatActivity.enableSchoolEdgeToEdge(useLightStatusBarIcons: Boolean = true) {
    enableEdgeToEdge(
        statusBarStyle = if (useLightStatusBarIcons) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        },
        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )
}
