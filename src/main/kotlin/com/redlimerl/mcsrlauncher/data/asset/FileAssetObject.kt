package com.redlimerl.mcsrlauncher.data.asset

import kotlinx.serialization.Serializable

@Serializable
data class FileAssetObject(
    val id: String,
    val sha1: String,
    val size: Long,
    val url: String
)