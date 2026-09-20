package com.lbs.schoolhelper.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens only standard HTTPS web links supplied by the school notice service. */
object ExternalUrlOpener {
    fun buildIntent(url: String): Intent? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        if (uri.scheme?.equals("https", ignoreCase = true) != true || uri.host.isNullOrBlank()) {
            return null
        }
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    fun open(context: Context, url: String): Boolean {
        val intent = buildIntent(url) ?: return false
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
