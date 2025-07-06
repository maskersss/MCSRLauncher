package com.redlimerl.mcsrlauncher.data.meta.library

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.network.FileDownloader
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class MetaLibrary(
    val name: String,
    val url: String
) {

    fun getPath(): Path {
        return AssetUtils.libraryNameToPath(GameAssetManager.LIBRARIES_PATH, this.name)
    }

    fun download(worker: LauncherWorker) {
        val jarFile = this.getPath().toFile()
        if (jarFile.exists()) return

        MCSRLauncher.LOGGER.info("Downloading $name ...")
        FileDownloader.download("$url${AssetUtils.getLibraryMavenPath(this.name)}", jarFile)
    }
}