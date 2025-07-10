package com.redlimerl.mcsrlauncher.data.meta

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.meta.file.MetaVersionFile
import com.redlimerl.mcsrlauncher.data.serializer.ISO8601Serializer
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SpeedrunUtils
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.net.URI
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class MetaVersionIndexes(
    val uid: MetaUniqueID,
    val name: String,
    val formatVersion: Int,
    val versions: List<MetaVersion>
) {
    companion object {
        fun getPath(metaUniqueID: MetaUniqueID): Path {
            return MetaManager.BASE_PATH.resolve(metaUniqueID.value).resolve("index.json")
        }
    }

    fun write() {
        FileUtils.writeStringToFile(getPath(this.uid).toFile(), MCSRLauncher.JSON.encodeToString(this), Charsets.UTF_8)
    }
}

@Serializable
data class MetaVersion(
    val version: String,
    @Serializable(with = ISO8601Serializer::class) val releaseTime: Date,
    val type: MetaVersionType = MetaVersionType.RELEASE,
    val requires: List<MetaDependency> = listOf(),
    val conflicts: List<MetaDependency> = listOf(),
    val recommended: Boolean = false,
    val sha256: String,
    val compatibleIntermediaries: List<IntermediaryType> = listOf()
) {
    fun getURI(uid: MetaUniqueID): URI {
        return MetaManager.getMetaURI().resolve(uid.value + "/").resolve("$version.json")
    }

    fun updateFile(uid: MetaUniqueID, worker: LauncherWorker): MetaVersionFile {
        MCSRLauncher.LOGGER.info("Updating meta version($version) file...")
        val response = MCSRLauncher.makeJsonSha256Request(HttpGet(this.getURI(uid)), worker)
        if (!response.hasSuccess()) throw IllegalStateException("Failed to get meta version file")

        val versionFile = response.get<MetaVersionFile>()
        versionFile.write()
        GameAssetManager.updateChecksum(this.getURI(uid).path, this.sha256)
        return versionFile
    }

    inline fun <reified T : MetaVersionFile> getOrLoadMetaVersionFile(uid: MetaUniqueID, worker: LauncherWorker): T {
        val file = MetaVersionFile.getPath(uid, this.version).toFile()
        if (!file.exists() || GameAssetManager.getChecksum(this.getURI(uid).path) != this.sha256) return updateFile(uid, worker) as T
        return MCSRLauncher.JSON.decodeFromString<T>(FileUtils.readFileToString(file, Charsets.UTF_8))
    }

    fun dataArray(): Array<String> {
        return arrayOf(
            I18n.translate("version." + type.name.lowercase()).let { if (SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.contains(this.version)) "$it ★" else it },
            this.version,
            SimpleDateFormat("MMMM dd, yyyy").format(this.releaseTime)
        )
    }
}