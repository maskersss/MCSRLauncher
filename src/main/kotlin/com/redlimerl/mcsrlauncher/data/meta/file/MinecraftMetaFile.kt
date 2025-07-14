package com.redlimerl.mcsrlauncher.data.meta.file

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.asset.GameAssetObject
import com.redlimerl.mcsrlauncher.data.asset.game.GameLibrary
import com.redlimerl.mcsrlauncher.data.asset.game.GameLogger
import com.redlimerl.mcsrlauncher.data.meta.LauncherTrait
import com.redlimerl.mcsrlauncher.data.meta.MetaDependency
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.MetaVersionType
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MinecraftMetaFile(
    override val uid: MetaUniqueID,
    override val name: String,
    override val formatVersion: Int,
    override val version: String,
    @Serializable(with = ISO8601Serializer::class) override val releaseTime: Date,
    val type: MetaVersionType,
    val requires: List<MetaDependency>,
    @SerialName("+traits") val traits: List<LauncherTrait> = listOf(),
    val compatibleJavaMajors: List<Int>,
    val compatibleJavaName: String? = null,
    val mainClass: String,
    val minecraftArguments: String,
    val mainJar: GameLibrary,
    val logging: GameLogger? = null,
    val assetIndex: GameAssetObject,
    val libraries: List<GameLibrary> = listOf(),
) : MetaVersionFile() {

    override fun install(worker: LauncherWorker) {
        MCSRLauncher.LOGGER.info("installing Minecraft assets/libraries...")

        worker.setState("Downloading game client .jar...")
        worker.setProgress(null)
        this.mainJar.download(worker)

        worker.setState("Downloading game libraries...")
        for ((installed, library) in this.libraries.withIndex()) {
            library.download(worker)
            worker.setProgress((installed + 1) / this.libraries.size.toFloat())
        }

        worker.setState("Downloading game asset indexes...")
        this.assetIndex.downloadAll(worker)
    }

}