package com.lbs.schoolhelper.telemetry

import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.remote.NeisApiException
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class TelemetryReporterTest {
    private val school = StudentInfo("2", "5", "개인 정보가 될 학교명", "J10", "1234567", "중학교")
    private val sink = RecordingSink()
    private var current = school
    private val device = DeviceContext(
        manufacturer = "Google",
        model = "Pixel 8",
        osSdk = 36,
        appVersion = "1.0-qa"
    )
    private val reporter = TelemetryReporter(sink, { current }, device, "qa")

    @Test fun `no consent emits no events or diagnostic context`() {
        reporter.appOpened(EntryPoint.LAUNCHER)
        reporter.featureViewed(Feature.MEALS)
        reporter.dataLoaded(Feature.MEALS, school, LoadOutcome.FAILURE, DataSource.NETWORK, 10, IllegalStateException("secret"))
        assertTrue(sink.events.isEmpty())
        assertTrue(sink.reports.isEmpty())
        assertTrue(sink.contexts.isEmpty())
    }

    @Test fun `analytics consent emits only allowlisted school context`() {
        reporter.setCollection(true, false)
        reporter.appOpened(EntryPoint.LAUNCHER)
        val event = sink.events.single()
        assertEquals("app_open_context", event.first)
        assertEquals(mapOf("office_code" to "J10", "school_code" to "1234567", "school_kind" to "middle",
            "setup_complete" to 1L, "environment" to "qa", "entry_point" to "launcher",
            "device_manufacturer" to "Google", "device_model" to "Pixel 8",
            "os_sdk" to 36L, "app_version" to "1.0-qa"), event.second)
        assertTrue(sink.reports.isEmpty())
        assertTrue(sink.contexts.isEmpty())
    }

    @Test fun `school changes refresh context and clearing removes previous school`() {
        reporter.setCollection(true, true)
        val previous = current
        current = school.copy(officeCode = "B10", schoolCode = "7654321")
        reporter.schoolSaved(previous)
        assertEquals("school_changed", sink.events.last().first)
        assertEquals("7654321", sink.contexts.last()["school_code"])
        current = StudentInfo()
        reporter.appOpened(EntryPoint.UNKNOWN)
        assertEquals("", sink.contexts.last()["school_code"])
        assertEquals(0L, sink.events.last().second["setup_complete"])
    }

    @Test fun `diagnostics without analytics sanitizes error and uses request school snapshot`() {
        reporter.setCollection(false, true)
        current = school.copy(schoolCode = "7654321")
        val error = NeisApiException("ERROR-300", "https://server/?KEY=secret&CLASS=5")
        reporter.dataLoaded(Feature.MEALS, school, LoadOutcome.FAILURE, DataSource.NETWORK, 32, error)
        val report = sink.reports.single()
        assertEquals("meals:neis_ERROR-300", report.first.message)
        assertNull(report.first.cause)
        assertFalse(report.first.toString().contains("secret"))
        assertArrayEquals(error.stackTrace, report.first.stackTrace)
        assertEquals("1234567", report.second["school_code"])
        assertEquals(32L, report.second["duration_ms"])
        assertTrue(sink.events.isEmpty())
    }

    @Test fun `expected offline and cancellation do not become nonfatals`() {
        reporter.setCollection(true, true)
        reporter.dataLoaded(Feature.MEALS, school, LoadOutcome.FAILURE, DataSource.NETWORK, 1, IOException("secret"))
        reporter.dataLoaded(Feature.MEALS, school, LoadOutcome.FAILURE, DataSource.NETWORK, 2, CancellationException("cancel"))
        assertEquals(1, sink.events.size)
        assertEquals("network", sink.events.single().second["error_code"])
        assertTrue(sink.reports.isEmpty())
    }

    @Test fun `duplicate nonfatals are bounded but all result events are retained`() {
        reporter.setCollection(true, true)
        repeat(20) { reporter.dataLoaded(Feature.MEALS, school, LoadOutcome.FAILURE, DataSource.NETWORK, 1, IllegalStateException("secret-$it")) }
        assertEquals(1, sink.reports.size)
        assertEquals(20, sink.events.size)
    }

    @Test fun `revocation immediately stops both channels and clears sink state`() {
        reporter.setCollection(true, true)
        reporter.featureViewed(Feature.MEALS)
        reporter.setCollection(false, false)
        val before = sink.events.size
        reporter.appOpened(EntryPoint.LAUNCHER)
        assertEquals(before, sink.events.size)
        assertEquals(false to false, sink.collection.last())
        assertEquals(2, sink.clears)
    }

    @Test fun `empty data stays a normal cache result`() {
        reporter.setCollection(true, true)
        reporter.dataLoaded(Feature.MEALS, school, LoadOutcome.EMPTY, DataSource.CACHE, 0)
        assertEquals("empty", sink.events.single().second["outcome"])
        assertEquals("cache", sink.events.single().second["source"])
        assertTrue(sink.reports.isEmpty())
    }

    @Test fun `malformed identity cannot inject arbitrary strings into context`() {
        reporter.setCollection(true, false)
        current = school.copy(officeCode = "secret@example.com", schoolCode = "https://example.com/key")
        reporter.appOpened(EntryPoint.UNKNOWN)
        assertEquals("", sink.events.single().second["office_code"])
        assertEquals("", sink.events.single().second["school_code"])
    }

    @Test fun `app open device context is bounded and excludes stable device identifiers`() {
        val reporter = TelemetryReporter(
            sink,
            { school },
            DeviceContext("Sam\$sung/전자", "Model\n" + "x".repeat(100), 36, "1.0 qa+1"),
            "qa"
        )
        reporter.setCollection(true, true)
        reporter.appOpened(EntryPoint.LAUNCHER)

        val parameters = sink.events.single().second
        assertEquals("Sam_sung___", parameters["device_manufacturer"])
        assertEquals(48, (parameters["device_model"] as String).length)
        assertEquals("1.0 qa_1", parameters["app_version"])
        assertFalse(parameters.keys.any { it.contains("id", ignoreCase = true) && it !in setOf("office_code", "school_code") })
        assertEquals(parameters["device_model"], sink.contexts.last()["device_model"])
    }

    @Test fun `sink errors never break user actions`() {
        val broken = object : TelemetrySink by sink {
            override fun event(name: String, parameters: Map<String, Any>) { throw IllegalStateException("SDK unavailable") }
        }
        val safe = TelemetryReporter(broken, { school }, device, "test")
        safe.setCollection(true, true)
        safe.appOpened(EntryPoint.LAUNCHER)
        safe.featureViewed(Feature.MEALS)
    }

    @Test(expected = CancellationException::class)
    fun `safe telemetry preserves cancellation`() {
        val telemetry = SafeTelemetry(object : AppTelemetry {
            override fun featureViewed(feature: Feature) {
                throw CancellationException("cancel")
            }
        })

        telemetry.featureViewed(Feature.MEALS)
    }
}

internal class RecordingSink : TelemetrySink {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val reports = mutableListOf<Pair<Throwable, Map<String, Any>>>()
    val contexts = mutableListOf<Map<String, Any>>()
    val collection = mutableListOf<Pair<Boolean, Boolean>>()
    var clears = 0
    override fun event(name: String, parameters: Map<String, Any>) { events += name to parameters }
    override fun context(parameters: Map<String, Any>) { contexts += parameters }
    override fun exception(error: Throwable, parameters: Map<String, Any>) { reports += error to parameters }
    override fun collection(analytics: Boolean, diagnostics: Boolean) { collection += analytics to diagnostics }
    override fun clearAnalytics() { clears++ }
    override fun clearDiagnostics() { clears++ }
}
