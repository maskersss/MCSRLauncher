package com.redlimerl.mcsrlauncher.data.asset.java

import kotlinx.serialization.Serializable

@Serializable
data class JavaRuntimeManifest(val files: Map<String, JavaRuntimeFile>)

@Serializable
data class JavaRuntimeFile(
    val downloads: Map<String, JavaRuntimeDownload> = mapOf(),
    val type: String
)

@Serializable
data class JavaRuntimeDownload(
    val sha1: String,
    val size: Long,
    val url: String
)