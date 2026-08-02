package org.openscanner.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** Languages intentionally shipped by Open Scanner's app module. */
enum class AppLanguage(val languageTag: String) {
    SYSTEM_DEFAULT(""),
    ENGLISH("en"),
    SIMPLIFIED_CHINESE("zh-CN"),
    ;

    companion object {
        fun fromLanguageTags(languageTags: String): AppLanguage {
            val primaryTag = languageTags
                .split(',')
                .firstOrNull()
                ?.trim()
                .orEmpty()
            if (primaryTag.isEmpty()) return SYSTEM_DEFAULT

            return when (Locale.forLanguageTag(primaryTag).language) {
                Locale.ENGLISH.language -> ENGLISH
                Locale.CHINESE.language -> SIMPLIFIED_CHINESE
                else -> SYSTEM_DEFAULT
            }
        }
    }
}

internal object AppLocaleController {
    fun currentLanguage(): AppLanguage = AppLanguage.fromLanguageTags(
        AppCompatDelegate.getApplicationLocales().toLanguageTags(),
    )

    fun setLanguage(language: AppLanguage) {
        val requestedLocales = LocaleListCompat.forLanguageTags(language.languageTag)
        if (requestedLocales.toLanguageTags() == AppCompatDelegate.getApplicationLocales().toLanguageTags()) {
            return
        }
        AppCompatDelegate.setApplicationLocales(requestedLocales)
    }
}
