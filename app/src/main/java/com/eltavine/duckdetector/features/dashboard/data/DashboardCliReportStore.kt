/*
 * Copyright 2026 Duck Apps Contributor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.features.dashboard.data

import android.content.Context
import android.util.Log
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardUiState
import org.json.JSONObject
import java.io.File

object DashboardCliReportStore {

    const val actionExportLatestReport: String =
        "com.eltavine.duckdetector.action.EXPORT_CLI_REPORT"

    private const val logTag = "DuckDetectorCli"
    private const val reportDirectoryName = "duck_detector_cli"
    private const val reportFileName = "latest_report.json"
    private const val maxLogChunkLength = 3000

    private val formatter = DashboardCliReportFormatter()

    fun persistLatestReport(
        context: Context,
        state: DashboardUiState,
        scanDurationMillis: Long?,
        scanCompletedAtEpochMillis: Long?,
    ): File {
        val report = formatter.format(
            state = state,
            packageName = context.packageName,
            scanDurationMillis = scanDurationMillis,
            scanCompletedAtEpochMillis = scanCompletedAtEpochMillis,
        )
        val reportFile = latestReportFile(context)
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report.toString(2))
        emitReport(
            report = report,
            reportFile = reportFile,
            reason = "scan_complete",
            packageName = context.packageName,
        )
        return reportFile
    }

    fun emitStoredReport(context: Context): Boolean {
        val reportFile = latestReportFile(context)
        if (!reportFile.exists()) {
            Log.w(
                logTag,
                "event=manual_export result=missing_report package=${context.packageName} hint=launch_app_and_wait_for_scan_completion",
            )
            return false
        }
        val reportText = runCatching { reportFile.readText() }.getOrElse { error ->
            Log.e(
                logTag,
                "event=manual_export result=read_failed package=${context.packageName} path=${reportFile.absolutePath} error=${error.message}",
            )
            return false
        }
        val report = runCatching { JSONObject(reportText) }.getOrElse { error ->
            Log.e(
                logTag,
                "event=manual_export result=parse_failed package=${context.packageName} path=${reportFile.absolutePath} error=${error.message}",
            )
            return false
        }
        emitReport(
            report = report,
            reportFile = reportFile,
            reason = "manual_export",
            packageName = context.packageName,
        )
        return true
    }

    fun latestReportFile(context: Context): File {
        return File(File(context.filesDir, reportDirectoryName), reportFileName)
    }

    private fun emitReport(
        report: JSONObject,
        reportFile: File,
        reason: String,
        packageName: String,
    ) {
        val compactJson = report.toString()
        val chunkCount = compactJson.chunkCount(maxLogChunkLength)
        Log.i(
            logTag,
            "event=$reason result=ok package=$packageName path=${reportFile.absolutePath} bytes=${compactJson.toByteArray().size} chunks=$chunkCount shell_hint=\"adb shell run-as $packageName cat files/$reportDirectoryName/$reportFileName\"",
        )
        compactJson.chunked(maxLogChunkLength).forEachIndexed { index, chunk ->
            Log.i(
                logTag,
                "json_chunk=${index + 1}/$chunkCount payload=$chunk",
            )
        }
    }

    private fun String.chunkCount(chunkLength: Int): Int {
        if (isEmpty()) return 0
        return (length + chunkLength - 1) / chunkLength
    }
}
