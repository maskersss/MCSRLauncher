package com.redlimerl.mcsrlauncher.data.meta.file

import com.redlimerl.mcsrlauncher.data.meta.MetaDependency
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.library.MetaLibrary
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class FabricLoaderMetaFile(
    override val uid: MetaUniqueID,
    override val name: String,
    override val formatVersion: Int,
    override val version: String,
    @Serializable(with = ISO8601Serializer::class) override val releaseTime: Date,
    val requires: List<MetaDependency>,
    val mainClass: String,
    @SerialName("+tweakers") val tweakers: List<String> = listOf(),
    val libraries: List<MetaLibrary>,
) : MetaVersionFile() {

    override fun install(worker: LauncherWorker) {
        worker.setState("Downloading Fabric Loader libraries...")

        val total = libraries.size
        val completed = AtomicInteger(0)

        AssetUtils.doConcurrency(libraries) {
            withContext(Dispatchers.IO) {
                it.download(worker)
            }
            val done = completed.incrementAndGet()
            worker.setProgress(done.toFloat() / total.toFloat())
        }
    }

}