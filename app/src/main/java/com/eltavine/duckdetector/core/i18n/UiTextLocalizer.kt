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

package com.eltavine.duckdetector.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

private enum class ChineseVariant {
    SIMPLIFIED,
    TRADITIONAL,
}

private data class PatternReplacement(
    val regex: Regex,
    val simplified: (MatchResult) -> String,
    val traditional: ((MatchResult) -> String)? = null,
) {
    fun replace(matchResult: MatchResult, variant: ChineseVariant): String {
        return when (variant) {
            ChineseVariant.SIMPLIFIED -> simplified(matchResult)
            ChineseVariant.TRADITIONAL -> (traditional ?: simplified)(matchResult)
        }
    }
}

@Composable
fun rememberLocalizedUiText(
    text: String,
): String {
    val configuration = LocalConfiguration.current
    val localeTags = remember(configuration) {
        ConfigurationCompat.getLocales(configuration).toLanguageTags()
    }
    val variant = remember(localeTags) {
        localeVariantFromTags(localeTags)
    }
    return remember(text, variant) {
        localizeUiText(text, variant)
    }
}

fun localizeUiText(
    text: String,
    locale: Locale,
): String {
    return localizeUiText(text, locale.toChineseVariant())
}

private fun localizeUiText(
    text: String,
    variant: ChineseVariant?,
): String {
    if (variant == null || text.isBlank()) {
        return text
    }

    exactTranslation(text, variant)?.let { return it }

    patternTranslations.firstNotNullOfOrNull { replacement ->
        replacement.regex.matchEntire(text)?.let { matchResult ->
            replacement.replace(matchResult, variant)
        }
    }?.let { return it }

    return text
}

private fun exactTranslation(
    text: String,
    variant: ChineseVariant,
): String? {
    return when (variant) {
        ChineseVariant.SIMPLIFIED -> simplifiedExact[text]
        ChineseVariant.TRADITIONAL -> traditionalExact[text] ?: simplifiedExact[text]
    }
}

private fun localizeFragment(
    text: String,
    variant: ChineseVariant,
): String {
    return exactTranslation(text, variant) ?: text
}

private fun localeVariantFromTags(
    localeTags: String,
): ChineseVariant? {
    val primary = localeTags.substringBefore(',').trim()
    if (primary.isBlank()) {
        return null
    }
    return when {
        primary.startsWith("zh-Hant", ignoreCase = true) ||
            primary.endsWith("-TW", ignoreCase = true) ||
            primary.endsWith("-HK", ignoreCase = true) ||
            primary.endsWith("-MO", ignoreCase = true) -> ChineseVariant.TRADITIONAL

        primary.startsWith("zh", ignoreCase = true) -> ChineseVariant.SIMPLIFIED
        else -> null
    }
}

private fun Locale.toChineseVariant(): ChineseVariant? {
    if (!language.equals("zh", ignoreCase = true)) {
        return null
    }
    val tag = toLanguageTag()
    return when {
        script.equals("Hant", ignoreCase = true) ||
            country.equals("TW", ignoreCase = true) ||
            country.equals("HK", ignoreCase = true) ||
            country.equals("MO", ignoreCase = true) ||
            tag.contains("Hant", ignoreCase = true) -> ChineseVariant.TRADITIONAL

        else -> ChineseVariant.SIMPLIFIED
    }
}

private val simplifiedExact = mapOf(
    "Security overview" to "安全总览",
    "Top findings" to "重点发现",
    "Priority review queue" to "优先复核队列",
    "Overview" to "总览",
    "Scan status" to "扫描状态",
    "Danger" to "高危",
    "Warning" to "警告",
    "Info" to "信息",
    "Ready" to "就绪",
    "Pending" to "进行中",
    "OK" to "正常",
    "Running local checks" to "正在执行本地检测",
    "Dashboard summary will unlock when the detector cards finish collecting evidence." to
        "检测卡片完成本地证据采集后，总览摘要会自动解锁。",
    "No urgent findings in ready modules" to "已完成模块中暂无紧急发现",
    "Open detector cards below to review detailed local evidence and secondary checks." to
        "展开下方检测卡片，可查看更详细的本地证据与二级校验结果。",
    "Waiting for detector evidence" to "等待检测器收集证据",
    "Detector cards will expand as modules finish collecting local evidence." to
        "各检测模块完成本地证据采集后，对应卡片会逐步补全。",
    "Report saved" to "报告已保存",
    "Export Report" to "导出报告",
    "Build Time (UTC)" to "构建时间 (UTC)",
    "Duck Detector QQ group" to "Duck Detector QQ 群",
    "Preparing startup" to "正在准备启动",
    "Loading agreement state before startup policy review." to "正在加载协议状态，随后进入启动策略检查。",
    "Device Info" to "设备信息",
    "Bootloader" to "Bootloader",
    "Kernel Check" to "内核检查",
    "Native Root" to "原生 Root",
    "System Properties" to "系统属性",
    "Custom ROM" to "第三方 ROM",
    "Dangerous Apps" to "高风险应用",
    "Memory" to "内存检测",
    "Mount" to "挂载检测",
    "SELinux" to "SELinux",
    "TEE" to "TEE",
    "LSPosed" to "LSPosed",
    "Zygisk" to "Zygisk",
    "Virtualization" to "虚拟化环境",
    "Play Integrity Fix" to "Play Integrity 修补",
    "Pending" to "进行中",
    "Error" to "错误",
    "Unavailable" to "不可用",
    "Clean" to "正常",
    "None" to "无",
    "Unsupported" to "不支持",
    "Loaded" to "已加载",
    "Limited" to "受限",
    "Partial" to "部分可用",
    "Inconclusive" to "无法判定",
    "Detected" to "已检测到",
    "Broken" to "已损坏",
    "Review" to "复核",
    "Scanning" to "扫描中",
    "Aligned" to "一致",
    "Aligned + review" to "一致但需复核",
    "Tampered" to "已篡改",
    "Mixed" to "混合",
    "Details" to "详情",
    "Certificates" to "证书",
    "Certificate chain" to "证书链",
    "Network" to "网络",
    "Boot state" to "启动状态",
    "Attestation" to "证明链",
    "Boot properties" to "启动属性",
    "Consistency" to "一致性",
    "Impact" to "影响",
    "Detection methods" to "检测方法",
    "Detection Methods" to "检测方法",
    "Scan summary" to "扫描摘要",
    "Build signals" to "构建信号",
    "Runtime signals" to "运行时信号",
    "Framework traces" to "框架痕迹",
    "Runtime checks" to "运行时检查",
    "Binder and services" to "Binder 与服务",
    "Packages and modules" to "包与模块",
    "Kernel identity" to "内核身份",
    "Anomalies" to "异常项",
    "Kernel behavior" to "内核行为",
    "Function hooks" to "函数 Hook",
    "Mappings and FD-backed code" to "映射与 FD 支撑代码",
    "Loader visibility" to "加载器可见性",
    "Native probes" to "原生探针",
    "Runtime artifacts" to "运行时痕迹",
    "Kernel traces" to "内核痕迹",
    "Property residue" to "属性残留",
    "Security and runtime" to "安全与运行时",
    "Verified boot" to "Verified Boot",
    "Build profile" to "构建画像",
    "Source consistency" to "来源一致性",
    "Cross-check rules" to "交叉校验规则",
    "Device info" to "设备信息",
    "Root artifacts" to "Root 痕迹",
    "Native context" to "原生上下文",
    "Spoof properties" to "伪装属性",
    "Cross-source consistency" to "跨来源一致性",
    "Runtime traces" to "运行时痕迹",
    "Runtime mounts" to "运行时挂载",
    "Filesystem" to "文件系统",
    "Namespace and consistency" to "命名空间与一致性",
    "Security state" to "安全状态",
    "Signals" to "信号",
    "References" to "参考信息",
    "Environment" to "环境",
    "Honeypots" to "诱饵项",
    "Host Apps" to "宿主应用",
    "Scan State" to "扫描状态",
    "Policy analysis" to "策略分析",
    "Audit integrity" to "审计完整性",
    "Reference" to "参考",
    "Packages" to "应用包",
    "Context" to "上下文",
    "Identity" to "身份",
    "Build" to "构建",
    "Android" to "Android",
    "Runtime" to "运行时",
    "State" to "状态",
    "Proof" to "证据",
    "Tier" to "等级",
    "Trust" to "信任",
    "Status" to "状态",
    "Critical" to "关键",
    "Bridge" to "桥接",
    "Hooks" to "Hook",
    "Naming" to "命名",
    "Boot" to "启动",
    "Behavior" to "行为",
    "Native" to "原生",
    "Flags" to "标记",
    "Direct" to "直接信号",
    "Mode" to "模式",
    "Policy" to "策略",
    "Audit" to "审计",
    "Artifacts" to "痕迹",
    "Daemons" to "守护进程",
    "Processes" to "进程",
    "Props" to "属性",
    "Coverage" to "覆盖度",
    "Confidence" to "置信度",
    "FD trap" to "FD 陷阱",
    "Verdict" to "结论",
    "Score" to "评分",
    "Targets" to "目标",
    "Hits" to "命中",
    "Hidden" to "隐藏",
    "No package hits" to "未命中任何应用包",
    "Scanning boot state and verified boot evidence" to "正在扫描启动状态与 Verified Boot 证据",
    "Bootloader scan failed" to "Bootloader 扫描失败",
    "Locked and attested verified" to "已锁定，且证明链验证通过",
    "Locked by boot properties" to "由启动属性判断为已锁定",
    "Locked state without full proof" to "锁定状态证据不足",
    "Boot state inconclusive" to "无法确定启动状态",
    "Attestation RootOfTrust, certificate trust, boot properties, raw androidboot parameters, and source consistency checks are collecting local evidence." to
        "正在收集 RootOfTrust、证书信任、启动属性、原始 androidboot 参数以及来源一致性的本地证据。",
    "Unlocked state, attestation contradictions, broken certificate trust, or verified-boot failures indicate reduced boot-chain trust." to
        "设备处于解锁状态、证明链互相矛盾、证书信任损坏或 Verified Boot 失败，说明启动链信任度下降。",
    "The boot chain is not obviously broken, but the evidence still shows custom-root, software-only, or coherence signals worth reviewing." to
        "启动链不一定已经损坏，但仍存在自定义 Root、纯软件证明或一致性异常等值得复核的信号。",
    "Boot properties look conservative, but the result falls back to software-readable signals because attestation RootOfTrust was unavailable." to
        "启动属性看起来较保守，但由于 RootOfTrust 不可用，本结果只能退回到软件可读信号。",
    "Neither attestation RootOfTrust nor readable boot properties exposed enough data for a confident bootloader verdict." to
        "无论是 RootOfTrust 还是可读启动属性，都不足以给出高置信度的 Bootloader 判断。",
    "Attestation and boot properties stayed aligned with a locked, verified boot chain." to
        "证明链与启动属性彼此一致，符合已锁定且通过 Verified Boot 的启动链状态。",
    "Gathering attestation, verified-boot, and property consistency evidence." to
        "正在收集证明链、Verified Boot 与属性一致性证据。",
    "Scanning kernel identity" to "正在扫描内核身份",
    "Kernel Check scan failed" to "内核检查失败",
    "Kernel behavior needs review" to "内核行为需要复核",
    "CVE patch state is informational" to "CVE 补丁状态仅供参考",
    "CVE patch state inconclusive" to "无法判断 CVE 补丁状态",
    "Kernel scan has reduced native coverage" to "内核扫描的原生覆盖度不足",
    "No suspicious kernel markers" to "未发现可疑内核标记",
    "Kernel naming, boot parameter, build-time, pointer-exposure, and Unicode path-bypass heuristics are collecting local evidence." to
        "正在收集内核命名、启动参数、构建时间、指针暴露与 Unicode 路径绕过启发式的本地证据。",
    "Kernel identity text or boot-time native checks surfaced markers commonly seen on modified or community-built kernels." to
        "内核标识文本或启动期原生检查出现了常见于魔改内核或社区内核的标记。",
    "Kernel behavior heuristics surfaced review-worthy signals, but they are weaker than direct naming or boot parameter anomalies." to
        "内核行为启发式发现了值得复核的信号，但强度弱于直接的命名异常或启动参数异常。",
    "The Unicode path-bypass probe suggests CVE-2024-43093 is not fully patched, but this is informational context rather than a kernel-compromise signal." to
        "Unicode 路径绕过探针提示 CVE-2024-43093 可能未完全修复，但这更偏向参考信息，并不直接代表内核被攻破。",
    "The Unicode path-bypass probe could not determine whether CVE-2024-43093 is fully patched on this device." to
        "Unicode 路径绕过探针无法确认这台设备是否已完整修复 CVE-2024-43093。",
    "Kernel identity, boot parameters, and behavior heuristics stayed within expected bounds." to
        "内核身份、启动参数与行为启发式都处于预期范围内。",
    "Scanning kernel-root indicators" to "正在扫描内核级 Root 指标",
    "Native Root scan failed" to "原生 Root 扫描失败",
    "KernelSU and APatch indicators detected" to "检测到 KernelSU 与 APatch 指标",
    "Current app already runs in KernelSU su domain" to "当前应用已运行在 KernelSU 的 su 域中",
    "KernelSU detected via ksu_driver" to "通过 ksu_driver 检测到 KernelSU",
    "KernelSU detected via prctl" to "通过 prctl 检测到 KernelSU",
    "KernelSU indicators detected" to "检测到 KernelSU 指标",
    "APatch indicators detected" to "检测到 APatch 指标",
    "Magisk native indicators detected" to "检测到 Magisk 原生指标",
    "Root indicators detected" to "检测到 Root 指标",
    "Isolated mount drift suggests namespace tampering" to "隔离挂载漂移提示命名空间可能被篡改",
    "Isolated-process namespace drift needs review" to "隔离进程命名空间漂移需要复核",
    "KernelSU manager weak fingerprint detected" to "检测到 KernelSU 管理器弱指纹",
    "KernelSU manager package detected" to "检测到 KernelSU 管理器应用包",
    "Native detector unavailable" to "原生探测器不可用",
    "Native root scan has reduced coverage" to "原生 Root 扫描覆盖度不足",
    "No native root indicators" to "未发现原生 Root 指标",
    "Native probes are collecting read-only supercall, syscall, side-channel, self-process, isolated-process mount drift, manager manifest, path, cgroup, kernel-string, and property evidence." to
        "正在收集只读 supercall、syscall、旁路信道、自身进程、隔离进程挂载漂移、管理器清单、路径、cgroup、内核字符串与属性等本地证据。",
    "Read-only ksu_driver hits, direct syscall hits, self-process IOC, root-manager paths, curated runtime residue paths, /data/local/tmp metadata drift, cgroup/process leakage, unexpected root processes, or isolated-process namespace drift indicate active native root infrastructure." to
        "只读 ksu_driver 命中、直接 syscall 命中、自身进程 IOC、Root 管理器路径、运行时残留路径、/data/local/tmp 元数据漂移、cgroup/进程泄露、异常 Root 进程或隔离进程命名空间漂移，都说明存在活动的原生 Root 基础设施。",
    "Only weaker isolated-process mount drift, manager manifest fingerprints, process, cgroup, kernel, property, or metadata residue surfaced. These are review-worthy, but not as strong as direct native probes." to
        "当前仅发现较弱的隔离进程挂载漂移、管理器指纹、进程、cgroup、内核、属性或元数据残留。这些值得复核，但强度不如直接原生探针。",
    "This detector relies mostly on JNI-backed native probes. Native coverage was unavailable on this build, and the remaining runtime checks stayed clean." to
        "该检测主要依赖 JNI 原生探针。当前构建缺少原生覆盖，但其余运行时检查保持正常。",
    "No native root indicator surfaced from available probes, but one or more direct, cgroup, isolated-process, or package-visibility evidence paths had reduced coverage." to
        "现有探针未发现原生 Root 指标，但直接探测、cgroup、隔离进程或包可见性路径中，有一条或多条证据链覆盖不足。",
    "Scanning property, boot, and source state" to "正在扫描属性、启动状态与来源状态",
    "System Properties scan failed" to "系统属性扫描失败",
    "System property scan has reduced coverage" to "系统属性扫描覆盖度不足",
    "No risky property or coherence drift" to "未发现高风险属性或一致性漂移",
    "Core security, verified boot, build profile, source consistency, and raw boot cross-checks are collecting local evidence." to
        "正在收集核心安全属性、Verified Boot、构建画像、来源一致性与原始启动交叉校验的本地证据。",
    "Property values, raw boot contradictions, cross-source drift, cross-check drift, or raw property-area residue indicate insecure build state, spoofing risk, or modified boot context." to
        "属性值、原始启动矛盾、跨来源漂移、交叉校验漂移或原始属性区残留，说明构建状态可能不安全，或存在伪装风险、启动上下文被修改。",
    "Cross-source drift, cross-property drift, or raw property-area residue suggests a review-worthy build or boot context, even if not every warning means active compromise." to
        "跨来源漂移、跨属性漂移或原始属性区残留说明构建或启动上下文值得复核，但并不代表每个警告都等于已被攻破。",
    "No risky property or coherence drift surfaced from available probes, but raw property-area layout coverage was unavailable." to
        "现有探针未发现高风险属性或一致性漂移，但原始属性区布局覆盖不可用。",
    "Key properties, framework constants, native libc reads, raw boot parameters, and property-area layout stayed aligned." to
        "关键属性、框架常量、原生 libc 读取、原始启动参数与属性区布局彼此一致。",
    "Scanning Zygisk runtime traces" to "正在扫描 Zygisk 运行时痕迹",
    "Zygisk scan failed" to "Zygisk 扫描失败",
    "Cross-process FD trap is positive" to "跨进程 FD 陷阱为阳性",
    "One heuristic probe needs review" to "有 1 项启发式探针需要复核",
    "No Zygisk runtime signal" to "未发现 Zygisk 运行时信号",
    "Zygisk result needs more support" to "Zygisk 结果需要更多证据支撑",
    "The detector is collecting cross-process specialization evidence first, then correlating environment, linker, namespace, maps, smaps, thread, fd, stack, seccomp, and heap traces from the current process." to
        "检测器会先收集跨进程特化证据，再关联当前进程中的环境、linker、命名空间、maps、smaps、线程、fd、栈、seccomp 与堆痕迹。",
    "FD trap or direct runtime probes exposed evidence consistent with TMP_PATH leakage, specialization tampering, namespace bypass, linker redirection, ptrace attachment, or libc-hook side effects." to
        "FD 陷阱或直接运行时探针发现了与 TMP_PATH 泄露、特化篡改、命名空间绕过、linker 重定向、ptrace 附着或 libc Hook 副作用一致的证据。",
    "Only heuristic residue surfaced, so this result should be read together with Memory and Mount before treating it as a confirmed Zygisk runtime." to
        "当前只发现启发式残留信号，因此应结合 Memory 与 Mount 一起判断，而不能直接当成已确认的 Zygisk 运行时。",
    "The FD trap stayed clean and the native runtime snapshot did not expose TMP_PATH, linker, maps, heap, thread, or descriptor traces associated with Zygisk-style injection." to
        "FD 陷阱保持正常，原生运行时快照也未暴露与 Zygisk 注入相关的 TMP_PATH、linker、maps、堆、线程或描述符痕迹。",
    "One or more major scan paths were unavailable, so this card cannot treat the absence of hits as a clean runtime result." to
        "一条或多条关键扫描路径不可用，因此本卡片不能把“没有命中”当成干净结果。",
    "Scanning SELinux state" to "正在扫描 SELinux 状态",
    "SELinux scan failed" to "SELinux 扫描失败",
    "Enforcing" to "强制模式",
    "Permissive" to "宽容模式",
    "Disabled" to "已禁用",
    "Unknown" to "未知",
    "Enforcing with audit rewrite" to "强制模式，但审计日志疑似被改写",
    "Enforcing with KSU context materialized" to "强制模式，但检测到 KSU 上下文实体化",
    "Enforcing with app_zygote seqno split" to "强制模式，但 app_zygote 存在 seqno 分裂",
    "Enforcing with app_zygote attr-write anomaly" to "强制模式，但 app_zygote 属性写入异常",
    "Enforcing with untrusted app_zygote carrier" to "强制模式，但 app_zygote 载体不可信",
    "Enforcing with reduced app_zygote coverage" to "强制模式，但 app_zygote 覆盖度不足",
    "Enforcing with unstable context oracle" to "强制模式，但上下文 Oracle 不稳定",
    "Enforcing with untrusted context oracle" to "强制模式，但上下文 Oracle 不可信",
    "Enforcing with context split" to "强制模式，但上下文存在分裂",
    "Enforcing with audit exposure" to "强制模式，但审计信息暴露",
    "Enforcing with weak policy" to "强制模式，但策略偏弱",
    "Enforcing with audit risk" to "强制模式，但存在审计风险",
    "Enforcing with policy drift" to "强制模式，但策略存在漂移",
    "Enforcing with minor drift" to "强制模式，但存在轻微漂移",
    "View target apps" to "查看目标应用",
)

private val traditionalExact = mapOf(
    "Security overview" to "安全總覽",
    "Top findings" to "重點發現",
    "Priority review queue" to "優先複核佇列",
    "Overview" to "總覽",
    "Scan status" to "掃描狀態",
    "Danger" to "高危",
    "Warning" to "警告",
    "Info" to "資訊",
    "Ready" to "就緒",
    "Pending" to "進行中",
    "Running local checks" to "正在執行本地檢測",
    "Dashboard summary will unlock when the detector cards finish collecting evidence." to
        "檢測卡片完成本地證據蒐集後，總覽摘要會自動解鎖。",
    "No urgent findings in ready modules" to "已完成模組中暫無緊急發現",
    "Open detector cards below to review detailed local evidence and secondary checks." to
        "展開下方檢測卡片，可查看更詳細的本地證據與次級校驗結果。",
    "Waiting for detector evidence" to "等待檢測器蒐集證據",
    "Detector cards will expand as modules finish collecting local evidence." to
        "各檢測模組完成本地證據蒐集後，對應卡片會逐步補全。",
    "Report saved" to "報告已保存",
    "Export Report" to "匯出報告",
    "Build Time (UTC)" to "建置時間 (UTC)",
    "Duck Detector QQ group" to "Duck Detector QQ 群",
    "Preparing startup" to "正在準備啟動",
    "Loading agreement state before startup policy review." to "正在載入協議狀態，隨後進入啟動策略檢查。",
    "Device Info" to "裝置資訊",
    "Kernel Check" to "核心檢查",
    "System Properties" to "系統屬性",
    "Custom ROM" to "第三方 ROM",
    "Dangerous Apps" to "高風險應用",
    "Memory" to "記憶體檢測",
    "Mount" to "掛載檢測",
    "Virtualization" to "虛擬化環境",
    "Play Integrity Fix" to "Play Integrity 修補",
    "Error" to "錯誤",
    "Unavailable" to "不可用",
    "Clean" to "正常",
    "Unsupported" to "不支援",
    "Loaded" to "已載入",
    "Limited" to "受限",
    "Partial" to "部分可用",
    "Inconclusive" to "無法判定",
    "Detected" to "已檢測到",
    "Broken" to "已損壞",
    "Review" to "複核",
    "Scanning" to "掃描中",
    "Details" to "詳情",
    "Certificates" to "憑證",
    "Certificate chain" to "憑證鏈",
    "Network" to "網路",
    "Impact" to "影響",
    "Detection methods" to "檢測方法",
    "Scan summary" to "掃描摘要",
    "Security state" to "安全狀態",
    "References" to "參考資訊",
    "Context" to "上下文",
    "State" to "狀態",
    "Proof" to "證據",
    "Tier" to "等級",
    "Trust" to "信任",
    "Status" to "狀態",
    "Critical" to "關鍵",
    "Confidence" to "置信度",
    "Verdict" to "結論",
    "Score" to "評分",
    "Targets" to "目標",
    "Hits" to "命中",
    "Hidden" to "隱藏",
    "No package hits" to "未命中任何應用包",
    "View target apps" to "查看目標應用",
)

private val patternTranslations = listOf(
    PatternReplacement(
        regex = Regex("^Scanned at (.+)\nTotal time (.+)$"),
        simplified = { "扫描时间 ${it.groupValues[1]}\n总耗时 ${it.groupValues[2]}" },
        traditional = { "掃描時間 ${it.groupValues[1]}\n總耗時 ${it.groupValues[2]}" },
    ),
    PatternReplacement(
        regex = Regex("^Start with (.+) and (.+)\\.$"),
        simplified = {
            "优先检查 ${localizeFragment(it.groupValues[1], ChineseVariant.SIMPLIFIED)} 和 ${localizeFragment(it.groupValues[2], ChineseVariant.SIMPLIFIED)}。"
        },
        traditional = {
            "優先檢查 ${localizeFragment(it.groupValues[1], ChineseVariant.TRADITIONAL)} 和 ${localizeFragment(it.groupValues[2], ChineseVariant.TRADITIONAL)}。"
        },
    ),
    PatternReplacement(
        regex = Regex("^Start with (.+)\\.$"),
        simplified = {
            "优先检查 ${localizeFragment(it.groupValues[1], ChineseVariant.SIMPLIFIED)}。"
        },
        traditional = {
            "優先檢查 ${localizeFragment(it.groupValues[1], ChineseVariant.TRADITIONAL)}。"
        },
    ),
    PatternReplacement(
        regex = Regex("^Review (.+) and (.+) next\\.$"),
        simplified = {
            "接下来请复核 ${localizeFragment(it.groupValues[1], ChineseVariant.SIMPLIFIED)} 和 ${localizeFragment(it.groupValues[2], ChineseVariant.SIMPLIFIED)}。"
        },
        traditional = {
            "接下來請複核 ${localizeFragment(it.groupValues[1], ChineseVariant.TRADITIONAL)} 和 ${localizeFragment(it.groupValues[2], ChineseVariant.TRADITIONAL)}。"
        },
    ),
    PatternReplacement(
        regex = Regex("^Review (.+) next\\.$"),
        simplified = {
            "接下来请复核 ${localizeFragment(it.groupValues[1], ChineseVariant.SIMPLIFIED)}。"
        },
        traditional = {
            "接下來請複核 ${localizeFragment(it.groupValues[1], ChineseVariant.TRADITIONAL)}。"
        },
    ),
    PatternReplacement(
        regex = Regex("^(.+) need more context before treating results as clean\\.$"),
        simplified = {
            "${localizeFragment(it.groupValues[1], ChineseVariant.SIMPLIFIED)} 还需要更多上下文，当前不能直接视为干净结果。"
        },
        traditional = {
            "${localizeFragment(it.groupValues[1], ChineseVariant.TRADITIONAL)} 還需要更多上下文，目前不能直接視為乾淨結果。"
        },
    ),
    PatternReplacement(
        regex = Regex("^Save failed: (.+)$"),
        simplified = { "保存失败：${it.groupValues[1]}" },
        traditional = { "保存失敗：${it.groupValues[1]}" },
    ),
    PatternReplacement(
        regex = Regex("^QQ group number copied: (.+)$"),
        simplified = { "已复制 QQ 群号：${it.groupValues[1]}" },
        traditional = { "已複製 QQ 群號：${it.groupValues[1]}" },
    ),
    PatternReplacement(
        regex = Regex("^View target apps \\((\\d+)\\)$"),
        simplified = { "查看目标应用 (${it.groupValues[1]})" },
        traditional = { "查看目標應用 (${it.groupValues[1]})" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) props · (\\d+) certs · (\\d+) cross-checks$"),
        simplified = { "${it.groupValues[1]} 项属性 · ${it.groupValues[2]} 张证书 · ${it.groupValues[3]} 项交叉校验" },
        traditional = { "${it.groupValues[1]} 項屬性 · ${it.groupValues[2]} 張憑證 · ${it.groupValues[3]} 項交叉校驗" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) critical boot integrity signal\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 个关键启动完整性信号" },
        traditional = { "${it.groupValues[1]} 個關鍵啟動完整性訊號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) boot state signal\\(s\\) need review$"),
        simplified = { "${it.groupValues[1]} 个启动状态信号需要复核" },
        traditional = { "${it.groupValues[1]} 個啟動狀態訊號需要複核" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) suspicious kernel signal\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 个可疑内核信号" },
        traditional = { "${it.groupValues[1]} 個可疑核心訊號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) high-risk property or coherence signal\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 个高风险属性或一致性信号" },
        traditional = { "${it.groupValues[1]} 個高風險屬性或一致性訊號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) property signal\\(s\\) need review$"),
        simplified = { "${it.groupValues[1]} 个属性信号需要复核" },
        traditional = { "${it.groupValues[1]} 個屬性訊號需要複核" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) runtime root signal\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 个运行时 Root 信号" },
        traditional = { "${it.groupValues[1]} 個執行期 Root 訊號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) native signal\\(s\\) need review$"),
        simplified = { "${it.groupValues[1]} 个原生信号需要复核" },
        traditional = { "${it.groupValues[1]} 個原生訊號需要複核" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) strong · (\\d+) heuristic · (\\d+) signal\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 个强信号 · ${it.groupValues[2]} 个启发式信号 · 共 ${it.groupValues[3]} 个信号" },
        traditional = { "${it.groupValues[1]} 個強訊號 · ${it.groupValues[2]} 個啟發式訊號 · 共 ${it.groupValues[3]} 個訊號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) direct runtime signal\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 个直接运行时信号" },
        traditional = { "${it.groupValues[1]} 個直接執行期訊號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) heuristic probes converged$"),
        simplified = { "${it.groupValues[1]} 个启发式探针结果收敛" },
        traditional = { "${it.groupValues[1]} 個啟發式探針結果收斂" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) rules · (\\d+) info · (\\d+) native · (\\d+) Build$"),
        simplified = { "${it.groupValues[1]} 条规则 · ${it.groupValues[2]} 项信息 · ${it.groupValues[3]} 项原生命中 · ${it.groupValues[4]} 项构建信号" },
        traditional = { "${it.groupValues[1]} 條規則 · ${it.groupValues[2]} 項資訊 · ${it.groupValues[3]} 項原生命中 · ${it.groupValues[4]} 項建置信號" },
    ),
    PatternReplacement(
        regex = Regex("^(\\d+) rules · (\\d+) info · (\\d+) native · (\\d+) Build · (\\d+) prop-area hole\\(s\\)$"),
        simplified = { "${it.groupValues[1]} 条规则 · ${it.groupValues[2]} 项信息 · ${it.groupValues[3]} 项原生命中 · ${it.groupValues[4]} 项构建信号 · ${it.groupValues[5]} 个属性区空洞" },
        traditional = { "${it.groupValues[1]} 條規則 · ${it.groupValues[2]} 項資訊 · ${it.groupValues[3]} 項原生命中 · ${it.groupValues[4]} 項建置信號 · ${it.groupValues[5]} 個屬性區空洞" },
    ),
)
