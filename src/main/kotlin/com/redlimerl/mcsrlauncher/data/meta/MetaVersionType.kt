package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MetaVersionType {
    @SerialName("release") RELEASE,
    @SerialName("version.beta") BETA,
    @SerialName("version.alpha") ALPHA,
    @SerialName("experiment") EXPERIMENT,
    @SerialName("snapshot") SNAPSHOT,
    @SerialName("old_snapshot") OLD_SNAPSHOT,
    @SerialName("old_beta") OLD_BETA,
    @SerialName("old_alpha") OLD_ALPHA;

    fun toTypeId(): String {
        return this.name.lowercase()
    }
}