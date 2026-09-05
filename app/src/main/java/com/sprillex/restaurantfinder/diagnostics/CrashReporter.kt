package com.sprillex.restaurantfinder.diagnostics

import android.app.Application
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {

    /**
     * Installs an uncaught exception handler that generates a standardized
     * crash report and launches the Android Share Sheet.
     *
     * Call as the very first line of Application.onCreate() or attachBaseContext().
     */
    fun install(app: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val versionName = try {
                    app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "Unknown"
                } catch (_: Exception) {
                    "Unknown"
                }

                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val stackTrace = throwable.stackTraceToString()

                // Construct standardized plaintext payload
                val reportBody = buildReportBody(
                    packageName = app.packageName,
                    versionName = versionName,
                    manufacturer = Build.MANUFACTURER,
                    model = Build.MODEL,
                    sdkInt = Build.VERSION.SDK_INT,
                    release = Build.VERSION.RELEASE,
                    threadName = thread.name,
                    threadId = thread.id,
                    timestamp = timestamp,
                    exceptionClass = throwable.javaClass.name,
                    message = throwable.message ?: "None",
                    stackTrace = stackTrace
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Crash: ${app.packageName} ($versionName)")
                    putExtra(Intent.EXTRA_TEXT, reportBody)
                    // Explicit extras for zero-ambiguity ingestion by Orchestrator
                    putExtra("EXTRA_CRASH_PAYLOAD", reportBody)
                    putExtra("EXTRA_PACKAGE_NAME", app.packageName)
                    putExtra("EXTRA_STACK_TRACE", stackTrace)
                }

                val chooser = Intent.createChooser(sendIntent, "Share Crash Report...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                app.startActivity(chooser)

                // Yield briefly to ensure the WindowManager schedules the chooser display
                Thread.sleep(800)
            } catch (_: Throwable) {
                // Fail silently to guarantee default termination executes
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun buildReportBody(
        packageName: String,
        versionName: String,
        manufacturer: String,
        model: String,
        sdkInt: Int,
        release: String,
        threadName: String,
        threadId: Long,
        timestamp: String,
        exceptionClass: String,
        message: String,
        stackTrace: String
    ): String {
        return buildString {
            appendLine("=== APP CRASH REPORT ===")
            appendLine("Package: $packageName")
            appendLine("Version: $versionName")
            appendLine("Device: $manufacturer $model (SDK $sdkInt, Android $release)")
            appendLine("Thread: $threadName (ID: $threadId)")
            appendLine("Timestamp: $timestamp")
            appendLine("Exception: $exceptionClass")
            appendLine("Message: $message")
            appendLine("--- STACK TRACE ---")
            appendLine(stackTrace)
            appendLine("=== END CRASH REPORT ===")
        }
    }
}
