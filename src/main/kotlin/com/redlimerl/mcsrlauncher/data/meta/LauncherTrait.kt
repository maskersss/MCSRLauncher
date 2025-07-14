package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LauncherTrait {
    @SerialName("FirstThreadOnMacOS") FIRST_THREAD_MACOS,
    @SerialName("legacyLaunch") LEGACY_LAUNCH,
}