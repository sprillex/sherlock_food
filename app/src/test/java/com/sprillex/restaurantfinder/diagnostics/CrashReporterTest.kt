package com.sprillex.restaurantfinder.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {

    @Test
    fun buildReportBody_containsStandardizedHeadersAndFields() {
        val body = CrashReporter.buildReportBody(
            packageName = "com.sprillex.restaurantfinder",
            versionName = "1.0",
            manufacturer = "Google",
            model = "Pixel 7",
            sdkInt = 34,
            release = "14",
            threadName = "main",
            threadId = 1L,
            timestamp = "2025-01-01 12:00:00",
            exceptionClass = "java.lang.NullPointerException",
            message = "Test exception message",
            stackTrace = "java.lang.NullPointerException: Test exception message\n\tat com.example.Test.run(Test.kt:10)"
        )

        assertTrue(body.contains("=== APP CRASH REPORT ==="))
        assertTrue(body.contains("Package: com.sprillex.restaurantfinder"))
        assertTrue(body.contains("Version: 1.0"))
        assertTrue(body.contains("Device: Google Pixel 7 (SDK 34, Android 14)"))
        assertTrue(body.contains("Thread: main (ID: 1)"))
        assertTrue(body.contains("Timestamp: 2025-01-01 12:00:00"))
        assertTrue(body.contains("Exception: java.lang.NullPointerException"))
        assertTrue(body.contains("Message: Test exception message"))
        assertTrue(body.contains("--- STACK TRACE ---"))
        assertTrue(body.contains("java.lang.NullPointerException: Test exception message"))
        assertTrue(body.contains("=== END CRASH REPORT ==="))
    }

    @Test
    fun regexParsingFallback_extractsPackageAndStackTrace() {
        val samplePackage = "com.sprillex.restaurantfinder"
        val sampleStackTrace = "java.lang.RuntimeException: Uncaught failure\n\tat com.sprillex.Main.onCreate(Main.kt:25)"

        val body = CrashReporter.buildReportBody(
            packageName = samplePackage,
            versionName = "1.0",
            manufacturer = "Generic",
            model = "Device",
            sdkInt = 33,
            release = "13",
            threadName = "main",
            threadId = 1L,
            timestamp = "2025-01-01 12:00:00",
            exceptionClass = "java.lang.RuntimeException",
            message = "Uncaught failure",
            stackTrace = sampleStackTrace
        )

        val packageRegex = "(?<=Package:\\s)([a-zA-Z0-9._]+)".toRegex()
        val stackTraceRegex = "(?s)(?<=--- STACK TRACE ---\n)(.*?)(?=\n=== END CRASH REPORT ===)".toRegex()

        val extractedPackage = packageRegex.find(body)?.value
        val extractedStackTrace = stackTraceRegex.find(body)?.value

        assertTrue(extractedPackage == samplePackage)
        assertTrue(extractedStackTrace == sampleStackTrace)
    }
}
