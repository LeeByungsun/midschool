package com.lbs.schoolhelper.widget

import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.repository.WidgetSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetProfileResolverTest {
    @Test
    fun resolver_missingSavedProfileFallsBackToActiveProfile() {
        val active = profile("active", "민준")
        val resolved = WidgetProfileResolver.resolve(WidgetSettings("deleted"), listOf(active), active)
        assertEquals(active, resolved)
    }

    @Test
    fun resolver_withoutAnyProfileReturnsNull() {
        assertNull(WidgetProfileResolver.resolve(WidgetSettings("deleted"), emptyList(), null))
    }

    private fun profile(id: String, name: String) = StudentProfile(
        id,
        name,
        StudentInfo("2", "3", "미사중학교", "J10", "1234567", "중학교")
    )
}
