package com.redlimerl.mcsrlauncher.data.instance.draftout

import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.instance.mod.ModData
import com.redlimerl.mcsrlauncher.network.FileDownloader
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import io.github.z4kn4fein.semver.toVersion
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class DraftoutVersionData(
    private val version: String,
    private val sha512: String,
    private val downloadUrl: String,
    val size: Long
) {
    fun download(instance: BasicInstance, worker: LauncherWorker) {
        val modFile = instance.getModsPath().resolve(this.downloadUrl.split("/").last() + ".temp").toFile()
        val oldMod: ModData? = instance.getMods().find { it.id == "draftout" }

        if (oldMod != null && oldMod.version.toVersion(false) >= this.version.toVersion(false)) return

        worker.setState("Downloading Draftout v${this.version}...")
        modFile.parentFile.mkdirs()
        FileDownloader.download(this.downloadUrl, modFile, worker, this.size)

        Files.move(modFile.toPath(), modFile.toPath().parent.resolve(this.downloadUrl.split("/").last()), StandardCopyOption.REPLACE_EXISTING)
        oldMod?.file?.delete()
    }
}