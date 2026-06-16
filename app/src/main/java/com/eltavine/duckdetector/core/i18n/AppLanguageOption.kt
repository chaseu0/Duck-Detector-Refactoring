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

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.eltavine.duckdetector.R

enum class AppLanguageOption(
    val localeTag: String?,
    @StringRes val labelResId: Int,
) {
    SYSTEM_DEFAULT(
        localeTag = null,
        labelResId = R.string.settings_language_option_system_default,
    ),
    ENGLISH(
        localeTag = "en",
        labelResId = R.string.settings_language_option_english,
    ),
    SIMPLIFIED_CHINESE(
        localeTag = "zh-CN",
        labelResId = R.string.settings_language_option_simplified_chinese,
    ),
    TRADITIONAL_CHINESE(
        localeTag = "zh-TW",
        labelResId = R.string.settings_language_option_traditional_chinese,
    ),
    ARABIC(
        localeTag = "ar",
        labelResId = R.string.settings_language_option_arabic,
    ),
    VIETNAMESE(
        localeTag = "vi",
        labelResId = R.string.settings_language_option_vietnamese,
    ),
    KOREAN(
        localeTag = "ko",
        labelResId = R.string.settings_language_option_korean,
    ),
    JAPANESE(
        localeTag = "ja",
        labelResId = R.string.settings_language_option_japanese,
    ),
    THAI(
        localeTag = "th",
        labelResId = R.string.settings_language_option_thai,
    ),
    UKRAINIAN(
        localeTag = "uk",
        labelResId = R.string.settings_language_option_ukrainian,
    ), ;

    fun toLocaleListCompat(): LocaleListCompat {
        return if (localeTag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
    }

    companion object {
        fun current(): AppLanguageOption {
            return fromLocaleTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }

        fun fromLocaleTags(localeTags: String): AppLanguageOption {
            if (localeTags.isBlank()) {
                return SYSTEM_DEFAULT
            }
            val normalized = localeTags.substringBefore(',').trim()
            return entries.firstOrNull { option ->
                option.localeTag?.equals(normalized, ignoreCase = true) == true ||
                    option.localeTag?.let { normalized.startsWith(it, ignoreCase = true) } == true ||
                    when (option) {
                        SIMPLIFIED_CHINESE -> normalized.startsWith("zh-Hans", ignoreCase = true) ||
                            normalized.equals("zh", ignoreCase = true)

                        TRADITIONAL_CHINESE -> normalized.startsWith("zh-Hant", ignoreCase = true)
                        else -> false
                    }
            } ?: SYSTEM_DEFAULT
        }
    }
}
