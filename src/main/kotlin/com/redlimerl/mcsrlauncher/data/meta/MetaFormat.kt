package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.Serializable

@Serializable
sealed class MetaFormat {
    abstract val formatVersion: Int
}