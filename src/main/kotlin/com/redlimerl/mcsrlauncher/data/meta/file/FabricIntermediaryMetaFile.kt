package com.redlimerl.mcsrlauncher.data.meta.file

import com.redlimerl.mcsrlauncher.data.meta.MetaDependency
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.MetaVersionType
import com.redlimerl.mcsrlauncher.data.meta.library.MetaLibrary
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class FabricIntermediaryMetaFile(
    override val uid: MetaUniqueID,
    override val name: String,
    override val formatVersion: Int,
    override val version: String,
    @Serializable(with = ISO8601Serializer::class) override val releaseTime: Date,
    val type: MetaVersionType = MetaVersionType.RELEASE,
    val requires: List<MetaDependency>,
    val volatile: Boolean,
    val libraries: List<MetaLibrary>,
) : MetaVersionFile() {

    override fun install(worker: LauncherWorker) {
        worker.setState("Downloading Fabric Intermediary libraries...")
        for ((installed, library) in this.libraries.withIndex()) {
            library.download(worker)
            worker.setProgress((installed + 1) / this.libraries.size.toFloat())
        }
    }

}