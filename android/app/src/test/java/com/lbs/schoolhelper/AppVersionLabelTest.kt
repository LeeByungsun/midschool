package com.lbs.schoolhelper

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionLabelTest {
    @Test
    fun `formats release and QA version names with v prefix`() {
        assertEquals("v1.0(5)", AppVersionLabel.format("1.0", 5))
        assertEquals("v1.0-qa(5)", AppVersionLabel.format("1.0-qa", 5))
    }
}
