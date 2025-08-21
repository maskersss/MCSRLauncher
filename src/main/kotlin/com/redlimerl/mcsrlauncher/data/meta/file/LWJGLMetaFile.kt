package com.redlimerl.mcsrlauncher.data.meta.file

import com.redlimerl.mcsrlauncher.data.asset.game.GameLibrary
import com.redlimerl.mcsrlauncher.data.meta.MetaDependency
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.MetaVersionType
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class LWJGLMetaFile(
    override val uid: MetaUniqueID,
    override val name: String,
    override val version: String,
    @Serializable(with = ISO8601Serializer::class) override val releaseTime: Date,
    override val formatVersion: Int,
    val type: MetaVersionType,
    val conflicts: List<MetaDependency>,
    val volatile: Boolean,
    val libraries: List<GameLibrary>,
) : MetaVersionFile() {

    override fun install(worker: LauncherWorker) {
        worker.setState("Downloading LWJGL libraries...")

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