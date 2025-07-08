package com.redlimerl.mcsrlauncher.data.launcher

import kotlinx.serialization.Serializable

@Serializable
enum class LauncherLanguage(val languageName: String, val localLanguageName: String, val languageCode: String) {
    ENGLISH("English", "English", "en_US"),
    KOREAN("Korean", "한국어", "ko_KR");

    override fun toString(): String {
        return "$languageName ($localLanguageName)"
    }
}