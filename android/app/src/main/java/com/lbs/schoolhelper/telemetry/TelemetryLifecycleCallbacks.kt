package com.lbs.schoolhelper.telemetry

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

class TelemetryLifecycleCallbacks(private val telemetry: AppTelemetry) : Application.ActivityLifecycleCallbacks {
    private val foreground = ForegroundSessionTracker(telemetry::appOpened)
    override fun onActivityStarted(activity: Activity) {
        val intent = activity.intent
        val entry = when (intent?.getStringExtra(ENTRY_POINT_EXTRA)) {
            "widget" -> EntryPoint.WIDGET
            "notification" -> EntryPoint.NOTIFICATION
            else -> if (intent?.action == Intent.ACTION_MAIN) EntryPoint.LAUNCHER else EntryPoint.UNKNOWN
        }
        foreground.started(entry)
    }
    override fun onActivityStopped(activity: Activity) = foreground.stopped(activity.isChangingConfigurations)
    override fun onActivityResumed(activity: Activity) {
        val feature = when (activity.javaClass.simpleName) {
            "MainActivity" -> Feature.HOME
            "SetupActivity" -> Feature.SETUP
            "SettingsActivity" -> Feature.SETTINGS
            "MealActivity" -> Feature.MEALS
            "TimetableActivity" -> Feature.TIMETABLE
            "ScheduleActivity" -> Feature.SCHEDULE
            "TimerActivity" -> Feature.TIMER
            "WidgetConfigActivity" -> Feature.WIDGET_SETTINGS
            else -> null
        }
        feature?.let(telemetry::featureViewed)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    companion object { const val ENTRY_POINT_EXTRA = "com.lbs.schoolhelper.telemetry.ENTRY_POINT" }
}
