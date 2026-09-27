package com.lbs.schoolhelper.widget

import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.WidgetSettings

object WidgetProfileResolver {
    fun resolve(
        settings: WidgetSettings,
        profiles: List<StudentProfile>,
        activeProfile: StudentProfile?
    ): StudentProfile? {
        return profiles.firstOrNull { it.id == settings.profileId } ?: activeProfile
    }
}
