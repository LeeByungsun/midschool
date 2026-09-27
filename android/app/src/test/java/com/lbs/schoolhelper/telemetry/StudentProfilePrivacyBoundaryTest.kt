package com.lbs.schoolhelper.telemetry

import com.lbs.schoolhelper.data.repository.StudentInfo
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentProfilePrivacyBoundaryTest {
    @Test
    fun `school telemetry never includes display name or profile id`() {
        val sink = RecordingSink()
        var current = StudentInfo("2", "3", "우리학교", "J10", "1234567", "중학교")
        val reporter = TelemetryReporter(
            sink = sink,
            studentInfo = { current },
            deviceContext = DeviceContext("Google", "Pixel", 36, "1.0"),
            environment = "test"
        )

        reporter.setCollection(true, true)
        reporter.appOpened(EntryPoint.LAUNCHER)
        reporter.dataLoaded(Feature.MEALS, current, LoadOutcome.SUCCESS, DataSource.NETWORK, 12)
        current = current.copy(schoolName = "둘째학교")
        reporter.schoolSaved(current, current)

        val serialized = (sink.events + sink.contexts.map { "context" to it })
            .toString()
        assertFalse(serialized.contains("우리학교"))
        assertFalse(serialized.contains("둘째학교"))
        assertFalse(serialized.contains("profile-id"))
        assertTrue(serialized.contains("1234567"))
    }

    @Test
    fun `switching profiles without saving a new school emits no school change`() {
        val sink = RecordingSink()
        val student = StudentInfo("2", "3", "우리학교", "J10", "1234567", "중학교")
        val reporter = TelemetryReporter(sink, { student }, DeviceContext("Google", "Pixel", 36, "1.0"), "test")
        reporter.setCollection(true, false)

        reporter.schoolSaved(student, student)

        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun `profile storage is excluded from cloud backup and device transfer`() {
        val backup = readResource("src/main/res/xml/backup_rules.xml")
        val extraction = readResource("src/main/res/xml/data_extraction_rules.xml")
        assertTrue(backup.contains("<exclude domain=\"sharedpref\" path=\"student_profiles.xml\" />"))
        assertTrue(extraction.contains("<exclude domain=\"sharedpref\" path=\"student_profiles.xml\" />"))
        assertTrue(extraction.indexOf("<cloud-backup>").let { start ->
            extraction.indexOf("student_profiles.xml", start) > start &&
                extraction.indexOf("</cloud-backup>", start) > extraction.indexOf("student_profiles.xml", start)
        })
        assertTrue(extraction.indexOf("<device-transfer>").let { start ->
            extraction.indexOf("student_profiles.xml", start) > start
        })
    }

    private fun readResource(path: String): String = String(Files.readAllBytes(Paths.get(path)))
}
