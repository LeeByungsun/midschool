package com.bsbarron.midschoolapp

import android.content.Context

object UserPreferences {
    private const val PREFS_NAME = "midschool_prefs"
    private const val KEY_GRADE = "grade"
    private const val KEY_CLASSROOM = "classroom"

    fun saveStudentInfo(context: Context, grade: String, classroom: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GRADE, grade)
            .putString(KEY_CLASSROOM, classroom)
            .apply()
    }

    fun getGrade(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GRADE, "") ?: ""
    }

    fun getClassroom(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CLASSROOM, "") ?: ""
    }

    fun hasStudentInfo(context: Context): Boolean {
        return getGrade(context).isNotBlank() && getClassroom(context).isNotBlank()
    }
}
