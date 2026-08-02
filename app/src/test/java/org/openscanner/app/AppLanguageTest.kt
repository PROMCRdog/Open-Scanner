package org.openscanner.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun emptyLocaleOverrideMeansSystemDefault() {
        assertEquals(AppLanguage.SYSTEM_DEFAULT, AppLanguage.fromLanguageTags(""))
    }

    @Test
    fun supportedEnglishTagsMapToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en-US"))
    }

    @Test
    fun supportedSimplifiedChineseTagsMapToSimplifiedChinese() {
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTags("zh-CN"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTags("zh-Hans-CN"))
    }

    @Test
    fun primaryLocaleDeterminesTheSelection() {
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTags("zh-CN,en"))
    }

    @Test
    fun unsupportedOverrideFailsClosedToSystemDefault() {
        assertEquals(AppLanguage.SYSTEM_DEFAULT, AppLanguage.fromLanguageTags("fr-FR"))
    }
}
