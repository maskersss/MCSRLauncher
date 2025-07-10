package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import kotlinx.serialization.Serializable

@Serializable
data class LWJGLVersionData(
    val type: MetaUniqueID,
    val version: String
)