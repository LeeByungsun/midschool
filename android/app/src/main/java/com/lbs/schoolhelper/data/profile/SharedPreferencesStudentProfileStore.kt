package com.lbs.schoolhelper.data.profile

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferencesStudentProfileStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) : StudentProfileStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): StudentProfilesData? {
        val json = preferences.getString(PROFILES_DATA_KEY, null) ?: return null
        return runCatching {
            gson.fromJson(json, StudentProfilesData::class.java)
        }.getOrNull()
    }

    override fun write(data: StudentProfilesData): Boolean {
        return preferences.edit()
            .putString(PROFILES_DATA_KEY, gson.toJson(data))
            .commit()
    }

    override fun clear(): Boolean {
        return preferences.edit()
            .remove(PROFILES_DATA_KEY)
            .commit()
    }

    companion object {
        private const val PREFERENCES_NAME = "student_profiles"
        private const val PROFILES_DATA_KEY = "profiles_data"
    }
}
