package com.lbs.schoolhelper.util

import android.view.View
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
