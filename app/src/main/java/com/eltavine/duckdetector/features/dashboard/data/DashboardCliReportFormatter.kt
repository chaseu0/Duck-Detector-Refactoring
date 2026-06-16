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

import com.eltavine.duckdetector.BuildConfig
import com.eltavine.duckdetector.core.ui.model.ContextItemModel
import com.eltavine.duckdetector.core.ui.model.DetectionSeverity
import com.eltavine.duckdetector.core.ui.model.DetectorStatus
import com.eltavine.duckdetector.core.ui.presentation.formatBuildTimeUtc
import com.eltavine.duckdetector.features.dangerousapps.ui.model.DangerousAppsCardModel
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardDetectorCardEntry
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardFindingModel
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardOverviewMetricModel
import com.eltavine.duckdetector.features.dashboard.ui.model.DashboardUiState
import com.eltavine.duckdetector.features.deviceinfo.ui.model.DeviceInfoCardModel
import com.eltavine.duckdetector.features.tee.domain.TeeCertificateItem
import com.eltavine.duckdetector.features.tee.ui.model.TeeCardModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DashboardCliReportFormatter {

    fun format(
        state: DashboardUiState,
        packageName: String,
        scanDurationMillis: Long?,
        scanCompletedAtEpochMillis: Long?,
        generatedAtEpochMillis: Long = System.currentTimeMillis(),
    ): JSONObject {
        return JSONObject()
            .put("schema_version", 1)
            .put("source", "dashboard_ui_state")
            .put("package_name", packageName)
            .put(
                "app",
                JSONObject()
                    .put("version_name", BuildConfig.VERSION_NAME)
                    .put("version_code", BuildConfig.VERSION_CODE)
                    .put("build_hash", BuildConfig.BUILD_HASH)
                    .put("build_time_utc", BuildConfig.BUILD_TIME_UTC)
                    .put("build_time_formatted_utc", formatBuildTimeUtc(BuildConfig.BUILD_TIME_UTC)),
            )
            .put(
                "scan",
                JSONObject()
                    .put("generated_at_epoch_ms", generatedAtEpochMillis)
                    .put("generated_at_iso_utc", epochMillisToIsoString(generatedAtEpochMillis))
                    .put("completed_at_epoch_ms", scanCompletedAtEpochMillis ?: JSONObject.NULL)
                    .put(
                        "completed_at_iso_utc",
                        scanCompletedAtEpochMillis?.let(::epochMillisToIsoString) ?: JSONObject.NULL,
                    )
                    .put("duration_ms", scanDurationMillis ?: JSONObject.NULL)
                    .put("is_loading", state.isLoading)
                    .put(
                        "overview",
                        JSONObject()
                            .put("title", state.overview.title)
                            .put("headline", state.overview.headline)
                            .put("summary", state.overview.summary)
                            .put("status", statusToJson(state.overview.status))
                            .put("metrics", metricsToJson(state.overview.metrics)),
                    )
                    .put("top_findings", findingsToJson(state.topFindings))
                    .put("detectors", detectorsToJson(state.detectorCards))
                    .put("device_info", deviceInfoToJson(state.deviceInfoCard)),
            )
    }

    private fun metricsToJson(metrics: List<DashboardOverviewMetricModel>): JSONArray {
        return JSONArray().apply {
            metrics.forEach { metric ->
                put(
                    JSONObject()
                        .put("label", metric.label)
                        .put("value", metric.value)
                        .put("status", statusToJson(metric.status)),
                )
            }
        }
    }

    private fun findingsToJson(findings: List<DashboardFindingModel>): JSONArray {
        return JSONArray().apply {
            findings.forEach { finding ->
                put(
                    JSONObject()
                        .put("detector_title", finding.detectorTitle)
                        .put("headline", finding.headline)
                        .put("detail", finding.detail)
                        .put("status", statusToJson(finding.status)),
                )
            }
        }
    }

    private fun detectorsToJson(entries: List<DashboardDetectorCardEntry>): JSONArray {
        return JSONArray().apply {
            entries.forEach { entry ->
                put(detectorToJson(entry))
            }
        }
    }

    private fun detectorToJson(entry: DashboardDetectorCardEntry): JSONObject {
        return when (entry) {
            is DashboardDetectorCardEntry.Bootloader -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "State" to detailRowsToJson(entry.model.stateRows),
                        "Attestation" to detailRowsToJson(entry.model.attestationRows),
                        "Properties" to detailRowsToJson(entry.model.propertyRows),
                        "Consistency" to detailRowsToJson(entry.model.consistencyRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.Mount -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Artifacts" to detailRowsToJson(entry.model.artifactRows),
                        "Runtime" to detailRowsToJson(entry.model.runtimeRows),
                        "Filesystem" to detailRowsToJson(entry.model.filesystemRows),
                        "Consistency" to detailRowsToJson(entry.model.consistencyRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.CustomRom -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Build" to detailRowsToJson(entry.model.buildRows),
                        "Runtime" to detailRowsToJson(entry.model.runtimeRows),
                        "Framework" to detailRowsToJson(entry.model.frameworkRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.Selinux -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "State" to detailRowsToJson(entry.model.stateRows),
                        "Policy" to detailRowsToJson(entry.model.policyRows),
                        "Audit" to detailRowsToJson(entry.model.auditRows),
                        "Device" to detailRowsToJson(entry.model.deviceRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))
                .put("policy_notes", stringItemsToJson(entry.model.policyNotes))
                .put("audit_notes", stringItemsToJson(entry.model.auditNotes))
                .put("references", stringsToJson(entry.model.references))

            is DashboardDetectorCardEntry.DangerousApps -> dangerousAppsToJson(entry.model, entry.id)

            is DashboardDetectorCardEntry.KernelCheck -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Identity" to detailRowsToJson(entry.model.identityRows),
                        "Anomalies" to detailRowsToJson(entry.model.anomalyRows),
                        "Behavior" to detailRowsToJson(entry.model.behaviorRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.Memory -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Hooks" to detailRowsToJson(entry.model.hookRows),
                        "Mapping" to detailRowsToJson(entry.model.mappingRows),
                        "Loader" to detailRowsToJson(entry.model.loaderRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.LSPosed -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Runtime" to detailRowsToJson(entry.model.runtimeRows),
                        "Binder" to detailRowsToJson(entry.model.binderRows),
                        "Package" to detailRowsToJson(entry.model.packageRows),
                        "SELinux policy" to detailRowsToJson(entry.model.policyRows),
                        "Native" to detailRowsToJson(entry.model.nativeRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.NativeRoot -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Native" to detailRowsToJson(entry.model.nativeRows),
                        "Runtime" to detailRowsToJson(entry.model.runtimeRows),
                        "Kernel" to detailRowsToJson(entry.model.kernelRows),
                        "Properties" to detailRowsToJson(entry.model.propertyRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.PlayIntegrityFix -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Properties" to detailRowsToJson(entry.model.propertyRows),
                        "Consistency" to detailRowsToJson(entry.model.consistencyRows),
                        "Native" to detailRowsToJson(entry.model.nativeRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.Tee -> teeToJson(entry.model, entry.id)

            is DashboardDetectorCardEntry.Su -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Artifacts" to detailRowsToJson(entry.model.artifactRows),
                        "Context" to detailRowsToJson(entry.model.contextRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.SystemProperties -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Core" to detailRowsToJson(entry.model.coreRows),
                        "Boot" to detailRowsToJson(entry.model.bootRows),
                        "Build" to detailRowsToJson(entry.model.buildRows),
                        "Source" to detailRowsToJson(entry.model.sourceRows),
                        "Consistency" to detailRowsToJson(entry.model.consistencyRows),
                        "Info" to detailRowsToJson(entry.model.infoRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))

            is DashboardDetectorCardEntry.Virtualization -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "Environment" to detailRowsToJson(entry.model.environmentRows),
                        "Runtime" to detailRowsToJson(entry.model.runtimeRows),
                        "Consistency" to detailRowsToJson(entry.model.consistencyRows),
                        "Honeypot" to detailRowsToJson(entry.model.honeypotRows),
                        "Host apps" to detailRowsToJson(entry.model.hostAppRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                        "Scan" to detailRowsToJson(entry.model.scanRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))
                .put("references", stringsToJson(entry.model.references))

            is DashboardDetectorCardEntry.Zygisk -> baseDetectorJson(
                id = entry.id,
                title = entry.model.title,
                subtitle = entry.model.subtitle,
                status = entry.model.status,
                verdict = entry.model.verdict,
                summary = entry.model.summary,
            ).put("header_facts", headerFactsToJson(entry.model.headerFacts))
                .put(
                    "sections",
                    sectionsToJson(
                        "State" to detailRowsToJson(entry.model.stateRows),
                        "Signals" to detailRowsToJson(entry.model.signalRows),
                        "Methods" to detailRowsToJson(entry.model.methodRows),
                    ),
                )
                .put("impact_items", impactItemsToJson(entry.model.impactItems))
                .put("references", stringsToJson(entry.model.references))
        }
    }

    private fun dangerousAppsToJson(
        model: DangerousAppsCardModel,
        id: String,
    ): JSONObject {
        val json = baseDetectorJson(
            id = id,
            title = model.title,
            subtitle = model.subtitle,
            status = model.status,
            verdict = model.verdict,
            summary = model.summary,
        ).put("header_facts", headerFactsToJson(model.headerFacts))
            .put("package_items", dangerousPackageItemsToJson(model.packageItems))
            .put("context", contextItemsToJson(model.context))
            .put("target_apps", dangerousTargetAppsToJson(model.targetApps))

        model.hmaAlert?.let { alert ->
            json.put(
                "hma_alert",
                JSONObject()
                    .put("title", alert.title)
                    .put("summary", alert.summary)
                    .put(
                        "hidden_packages",
                        JSONArray().apply {
                            alert.hiddenPackages.forEach { pkg ->
                                put(
                                    JSONObject()
                                        .put("app_name", pkg.appName)
                                        .put("package_name", pkg.packageName)
                                        .put("methods", stringsToJson(pkg.methods)),
                                )
                            }
                        },
                    ),
            )
        }
        return json
    }

    private fun teeToJson(
        model: TeeCardModel,
        id: String,
    ): JSONObject {
        return baseDetectorJson(
            id = id,
            title = model.title,
            subtitle = model.subtitle,
            status = model.status,
            verdict = model.verdict,
            summary = model.summary,
        ).put("finding_detail", model.findingDetail ?: JSONObject.NULL)
            .put("is_expanded", model.isExpanded)
            .put("header_facts", headerFactsToJson(model.headerFacts))
            .put(
                "highlight_signals",
                JSONArray().apply {
                    model.highlightSignals.forEach { signal ->
                        put(
                            JSONObject()
                                .put("label", signal.label)
                                .put("value", signal.value)
                                .put("status", statusToJson(signal.status)),
                        )
                    }
                },
            )
            .put(
                "fact_groups",
                JSONArray().apply {
                    model.factGroups.forEach { group ->
                        put(
                            JSONObject()
                                .put("title", group.title)
                                .put(
                                    "rows",
                                    JSONArray().apply {
                                        group.rows.forEach { row ->
                                            put(
                                                JSONObject()
                                                    .put("icon", row.icon.name)
                                                    .put("label", row.label)
                                                    .put("value", row.value)
                                                    .put("status", statusToJson(row.status))
                                                    .put(
                                                        "hidden_copy_text",
                                                        row.hiddenCopyText ?: JSONObject.NULL,
                                                    ),
                                            )
                                        }
                                    },
                                ),
                        )
                    }
                },
            )
            .put(
                "network_state",
                JSONObject()
                    .put("label", model.networkState.label)
                    .put("summary", model.networkState.summary)
                    .put("status", statusToJson(model.networkState.status)),
            )
            .put(
                "certificate_summary",
                JSONObject()
                    .put("label", model.certificateSummary.label)
                    .put("count", model.certificateSummary.count)
                    .put("certificates", certificatesToJson(model.certificateSummary.certificates)),
            )
            .put(
                "actions",
                JSONArray().apply {
                    model.actions.forEach { action ->
                        put(
                            JSONObject()
                                .put("id", action.id.name)
                                .put("label", action.label)
                                .put("counter", action.counter ?: JSONObject.NULL)
                                .put("enabled", action.enabled),
                        )
                    }
                },
            )
            .put("export_text", model.exportText)
            .put("rkp_badge_label", model.rkpBadgeLabel ?: JSONObject.NULL)
    }

    private fun deviceInfoToJson(model: DeviceInfoCardModel): JSONObject {
        return JSONObject()
            .put("title", model.title)
            .put("subtitle", model.subtitle)
            .put("status", statusToJson(model.status))
            .put("verdict", model.verdict)
            .put("summary", model.summary)
            .put("header_facts", headerFactsToJson(model.headerFacts))
            .put(
                "sections",
                JSONArray().apply {
                    model.sections.forEach { section ->
                        put(
                            JSONObject()
                                .put("title", section.title)
                                .put(
                                    "rows",
                                    JSONArray().apply {
                                        section.rows.forEach { row ->
                                            put(
                                                JSONObject()
                                                    .put("label", row.label)
                                                    .put("value", row.value)
                                                    .put("detail_monospace", row.detailMonospace),
                                            )
                                        }
                                    },
                                ),
                        )
                    }
                },
            )
    }

    private fun baseDetectorJson(
        id: String,
        title: String,
        subtitle: String,
        status: DetectorStatus,
        verdict: String,
        summary: String,
    ): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("subtitle", subtitle)
            .put("status", statusToJson(status))
            .put("verdict", verdict)
            .put("summary", summary)
    }

    private fun sectionsToJson(vararg sections: Pair<String, JSONArray>): JSONArray {
        return JSONArray().apply {
            sections.forEach { (title, rows) ->
                if (rows.length() > 0) {
                    put(
                        JSONObject()
                            .put("title", title)
                            .put("rows", rows),
                    )
                }
            }
        }
    }

    private fun headerFactsToJson(facts: List<*>): JSONArray {
        return JSONArray().apply {
            facts.forEach { fact ->
                val item = fact ?: return@forEach
                put(
                    JSONObject()
                        .put("label", stringField(item, "label"))
                        .put("value", stringField(item, "value"))
                        .put("status", statusField(item)),
                )
            }
        }
    }

    private fun detailRowsToJson(rows: List<*>): JSONArray {
        return JSONArray().apply {
            rows.forEach { row ->
                val item = row ?: return@forEach
                put(
                    JSONObject()
                        .put("label", stringField(item, "label"))
                        .put("value", stringField(item, "value"))
                        .put("detail", nullableStringField(item, "detail") ?: JSONObject.NULL)
                        .put("status", statusField(item))
                        .put(
                            "detail_monospace",
                            booleanField(item, "detailMonospace"),
                        ),
                )
            }
        }
    }

    private fun impactItemsToJson(items: List<*>): JSONArray {
        return JSONArray().apply {
            items.forEach { impact ->
                val item = impact ?: return@forEach
                put(
                    JSONObject()
                        .put("text", stringField(item, "text"))
                        .put("status", statusField(item)),
                )
            }
        }
    }

    private fun stringItemsToJson(items: List<*>): JSONArray {
        return JSONArray().apply {
            items.forEach { impact ->
                val item = impact ?: return@forEach
                put(stringField(item, "text"))
            }
        }
    }

    private fun dangerousPackageItemsToJson(items: List<*>): JSONArray {
        return JSONArray().apply {
            items.forEach { pkg ->
                val item = pkg ?: return@forEach
                put(
                    JSONObject()
                        .put("app_name", stringField(item, "appName"))
                        .put("package_name", stringField(item, "packageName"))
                        .put("methods", stringsToJson(listField(item, "methods"))),
                )
            }
        }
    }

    private fun dangerousTargetAppsToJson(items: List<*>): JSONArray {
        return JSONArray().apply {
            items.forEach { target ->
                val item = target ?: return@forEach
                put(
                    JSONObject()
                        .put("app_name", stringField(item, "appName"))
                        .put("package_name", stringField(item, "packageName"))
                        .put("category", stringField(item, "category")),
                )
            }
        }
    }

    private fun contextItemsToJson(items: List<ContextItemModel>): JSONArray {
        return JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject()
                        .put("label", item.label)
                        .put("value", item.value),
                )
            }
        }
    }

    private fun certificatesToJson(items: List<TeeCertificateItem>): JSONArray {
        return JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject()
                        .put("slot_label", item.slotLabel)
                        .put("subject", item.subject)
                        .put("issuer", item.issuer)
                        .put("serial_number", item.serialNumber)
                        .put("valid_from", item.validFrom)
                        .put("valid_until", item.validUntil)
                        .put("signature_algorithm", item.signatureAlgorithm)
                        .put("public_key_summary", item.publicKeySummary),
                )
            }
        }
    }

    private fun stringsToJson(items: List<String>): JSONArray {
        return JSONArray().apply {
            items.forEach(::put)
        }
    }

    private fun statusToJson(status: DetectorStatus): JSONObject {
        return JSONObject()
            .put("severity", status.severity.name)
            .put("severity_rank", severityRank(status.severity))
            .put("info_kind", status.infoKind?.name ?: JSONObject.NULL)
    }

    private fun severityRank(severity: DetectionSeverity): Int {
        return when (severity) {
            DetectionSeverity.ALL_CLEAR -> 0
            DetectionSeverity.INFO -> 1
            DetectionSeverity.WARNING -> 2
            DetectionSeverity.DANGER -> 3
        }
    }

    private fun statusField(instance: Any): Any {
        return try {
            val value = instance::class.java.getDeclaredField("status").apply {
                isAccessible = true
            }.get(instance) as DetectorStatus
            statusToJson(value)
        } catch (_: Exception) {
            JSONObject.NULL
        }
    }

    private fun stringField(
        instance: Any,
        fieldName: String,
    ): String {
        return instance::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
        }.get(instance) as String
    }

    private fun nullableStringField(
        instance: Any,
        fieldName: String,
    ): String? {
        return try {
            instance::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
            }.get(instance) as? String
        } catch (_: Exception) {
            null
        }
    }

    private fun booleanField(
        instance: Any,
        fieldName: String,
    ): Boolean {
        return try {
            instance::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
            }.getBoolean(instance)
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun listField(
        instance: Any,
        fieldName: String,
    ): List<String> {
        return instance::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
        }.get(instance) as List<String>
    }

    private fun epochMillisToIsoString(epochMillis: Long): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC),
        )
    }
}
