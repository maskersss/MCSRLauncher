package com.redlimerl.mcsrlauncher.data.meta

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.net.URI

@Serializable
data class MetaPackageIndexes(
    override val formatVersion: Int = 1,
    val packages: List<MetaPackage> = listOf(),
    var latestUpdate: Long = 0
) : MetaFormat() {

    fun getVersions(metaUniqueID: MetaUniqueID, worker: LauncherWorker): List<MetaVersion> {
        return (packages.find { it.uid == metaUniqueID } ?: throw IllegalArgumentException("$metaUniqueID is missing in package indexes"))
            .getVersions(worker).versions
    }

}

@Serializable
data class MetaPackage(
    val uid: MetaUniqueID,
    val name: String,
    val sha256: String
) {
    private fun getURI(): URI {
        return MetaManager.getMetaURI().resolve(this.uid.value)
    }

    fun updateVersions(worker: LauncherWorker): MetaVersionIndexes {
        worker.setState("Updating ${uid.name}-meta versions...")
        val response = MCSRLauncher.makeJsonSha256Request(HttpGet(this.getURI()), worker)
        if (!response.hasSuccess()) throw IllegalStateException("Failed to get meta versions")

        val indexes = response.get<MetaVersionIndexes>()
        indexes.write()
        GameAssetManager.updateChecksum(this.getURI().path, this.sha256)
        CACHE[this.uid] = indexes
        return indexes
    }

    fun getVersions(worker: LauncherWorker): MetaVersionIndexes {
        val file = MetaVersionIndexes.getPath(this.uid).toFile()
        if (!file.exists() || GameAssetManager.getChecksum(this.getURI().path) != this.sha256) return updateVersions(worker)


        return if (CACHE[this.uid] == null) {
            val versionIndexes = MCSRLauncher.JSON.decodeFromString<MetaVersionIndexes>(FileUtils.readFileToString(file, Charsets.UTF_8))
            CACHE[this.uid] = versionIndexes
            versionIndexes
        } else CACHE[this.uid]!!
    }

    companion object {
        private val CACHE = hashMapOf<MetaUniqueID, MetaVersionIndexes>()
    }
}