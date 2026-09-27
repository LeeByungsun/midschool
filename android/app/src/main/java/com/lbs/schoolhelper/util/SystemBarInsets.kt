package com.lbs.schoolhelper.util

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.widget.ScrollView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/**
 * Applies system bar insets while preserving XML padding.
 *
 * Edge-to-edge screens need bottom inset padding so the last scroll/list item can
 * rest above the soft navigation keys or software keyboard instead of being hidden.
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
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(
            initialLeft + if (applyLeft) systemBars.left else 0,
            initialTop + if (applyTop) systemBars.top else 0,
            initialRight + if (applyRight) systemBars.right else 0,
            initialBottom + if (applyBottom) max(systemBars.bottom, ime.bottom) else 0
        )
        if (ime.bottom > systemBars.bottom) {
            view.scrollFocusedChildAboveIme(ime.bottom)
        }
        insets
    }
}

private fun View.scrollFocusedChildAboveIme(imeBottom: Int) {
    val container = this as? ScrollView ?: return
    val focusedView = findFocus() ?: return
    container.scrollChildAboveIme(focusedView, imeBottom)
}

private fun ScrollView.scrollChildAboveIme(child: View, imeBottom: Int) {
    post {
        val focusedBounds = Rect().also(child::getDrawingRect)
        offsetDescendantRectToMyCoords(child, focusedBounds)

        val margin = (16 * resources.displayMetrics.density).toInt()
        val visibleBottom = scrollY + height - imeBottom - margin
        val hiddenHeight = focusedBounds.bottom - visibleBottom
        if (hiddenHeight > 0) {
            scrollBy(0, hiddenHeight)
        }
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

/** Keeps a focused form field visible after the software keyboard changes the viewport. */
fun View.requestVisibleAboveKeyboard() {
    postDelayed(
        {
            val scrollView = findAncestorScrollView()
            val rootImeBottom = ViewCompat.getRootWindowInsets(this)
                ?.getInsets(WindowInsetsCompat.Type.ime())
                ?.bottom
                ?: 0
            val imeBottom = max(rootImeBottom, scrollView?.paddingBottom ?: 0)
            if (imeBottom > 0 && scrollView != null) {
                scrollView.scrollChildAboveIme(this, imeBottom)
                return@postDelayed
            }

            val extraBottom = (16 * resources.displayMetrics.density).toInt()
            requestRectangleOnScreen(
                Rect(0, 0, width, height + extraBottom),
                true
            )
        },
        180L
    )
}

private fun View.findAncestorScrollView(): ScrollView? {
    var ancestor = parent
    while (ancestor is View) {
        if (ancestor is ScrollView) return ancestor
        ancestor = ancestor.parent
    }
    return null
}
