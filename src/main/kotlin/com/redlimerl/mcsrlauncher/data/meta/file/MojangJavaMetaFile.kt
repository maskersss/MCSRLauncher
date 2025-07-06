package com.redlimerl.mcsrlauncher.data.meta.file

import com.redlimerl.mcsrlauncher.data.device.RuntimeOSType
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MojangJavaMetaFile(
    override val uid: MetaUniqueID,
    override val name: String,
    override val formatVersion: Int,
    override val version: String,
    @Serializable(with = ISO8601Serializer::class) override val releaseTime: Date,
    val runtimes: List<MojangJavaRuntime>
) : MetaVersionFile() {
    override fun install(worker: LauncherWorker) {
        TODO("Not yet implemented")
    }
}

@Serializable
data class MojangJavaRuntime(
    val name: String,
    val runtimeOS: RuntimeOSType,
    val version: MojangJavaRuntimeVersion,
    @Serializable(with = ISO8601Serializer::class) val releaseTime: Date,
    val vendor: String,
    val packageType: String,
    val downloadType: String,
    val checksum: MojangJavaRuntimeChecksum,
    val url: String
)

@Serializable
data class MojangJavaRuntimeVersion(
    val name: String,
    val major: Int,
    val minor: Int? = null,
    val security: Int? = null,
    val build: Int? = null
)

@Serializable
data class MojangJavaRuntimeChecksum(
    val type: String,
    val hash: String
)