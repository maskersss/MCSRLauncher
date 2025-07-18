package com.redlimerl.mcsrlauncher.data.meta.mod

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SpeedrunModTrait {
    @SerialName("rsg-only") RSG,
    @SerialName("ssg-only") SSG,
    @SerialName("mac-only") MAC,
    @SerialName("accessibility") ACCESSIBILITY
}