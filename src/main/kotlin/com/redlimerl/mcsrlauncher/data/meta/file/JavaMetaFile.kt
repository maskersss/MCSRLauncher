package com.redlimerl.mcsrlauncher.data.meta.file

import com.redlimerl.mcsrlauncher.data.device.RuntimeOSType
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class JavaMetaFile(
    override val uid: MetaUniqueID,
    override val name: String,
    override val formatVersion: Int,
    override val version: String,
    @Serializable(with = ISO8601Serializer::class) override val releaseTime: Date,
    val runtimes: List<JavaRuntime>
) : MetaVersionFile() {
    override fun install(worker: LauncherWorker) {
        worker.setState("Downloading Java: $name - $version...")

        if (!worker.properties.containsKey("download-java-version")) throw IllegalStateException("Intermediary type isn't updated")

        val version = worker.properties["download-java-version"]
        for (runtime in runtimes) {
            if (runtime.runtimeOS.isOn() && version == runtime.version.getName()) {
                TODO("THIS")
            }
        }
    }
}

@Serializable
data class JavaRuntime(
    val name: String,
    val runtimeOS: RuntimeOSType,
    val version: JavaRuntimeVersion,
    @Serializable(with = ISO8601Serializer::class) val releaseTime: Date,
    val vendor: String,
    val packageType: String,
    val downloadType: String,
    val checksum: JavaRuntimeChecksum,
    val url: String
)

@Serializable
data class JavaRuntimeVersion(
    val major: Int,
    val minor: Int? = null,
    val security: Int? = null,
    val build: Int? = null
) {
    fun getName(): String {
        return buildString {
            append(major)
            append((if (minor != null) ".$minor" else ""))
            append((if (security != null) ".$security" else ""))
            append((if (build != null) "+$build" else ""))
        }
    }
}

@Serializable
data class JavaRuntimeChecksum(
    val type: String,
    val hash: String
)