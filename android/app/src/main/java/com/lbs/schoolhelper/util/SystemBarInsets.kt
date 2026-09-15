package com.lbs.schoolhelper.util

import android.graphics.Color
import android.content.res.Configuration
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
 * Configures edge-to-edge system bars for the app's current surface.
 *
 * Most screens use dark icons in light mode and light icons in dark mode. The
 * splash screen always opts into light icons because its surface is navy.
 */
fun AppCompatActivity.enableSchoolEdgeToEdge(
    useDarkSystemBarIcons: Boolean = !isNightMode()
) {
    enableEdgeToEdge(
        statusBarStyle = if (useDarkSystemBarIcons) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        },
        navigationBarStyle = if (useDarkSystemBarIcons) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
    )
}

private fun AppCompatActivity.isNightMode(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
