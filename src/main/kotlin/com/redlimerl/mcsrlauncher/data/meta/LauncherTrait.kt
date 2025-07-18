package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LauncherTrait {
    @SerialName("FirstThreadOnMacOS") FIRST_THREAD_MACOS,
    @SerialName("legacyLaunch") LEGACY_LAUNCH,
    @SerialName("noapplet") NO_APPLET,
    @SerialName("legacyServices") LEGACY_SERVICE,
    @SerialName("feature:is_quick_play_singleplayer") QUICK_PLAY_SINGLE,
    @SerialName("feature:is_quick_play_multiplayer") QUICK_PLAY_SERVER,
}