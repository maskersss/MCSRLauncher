package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.Serializable

@Serializable
data class MetaDependency(
    val uid: MetaUniqueID,
    val suggests: String? = null,
    val equals: String? = null
)
