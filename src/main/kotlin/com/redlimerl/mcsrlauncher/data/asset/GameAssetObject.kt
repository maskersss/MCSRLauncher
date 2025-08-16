package com.redlimerl.mcsrlauncher.data.asset

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.asset.game.GameAssetIndex
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.network.FileDownloader
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.HttpUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import org.apache.hc.client5.http.classic.methods.HttpGet

@Serializable
data class GameAssetObject(
    val id: String,
    private val sha1: String,
    private val size: Long,
    val totalSize: Long,
    private val url: String
) {
    fun getAssetIndexes(worker: LauncherWorker): GameAssetIndex {
        val assetIndexFile = GameAssetManager.INDEXES_PATH.resolve("${this.id}.json").toFile()
        if (!assetIndexFile.exists() || GameAssetManager.getChecksum(this.url) != this.sha1) {
            val assetIndexResponse = HttpUtils.makeJsonRequest(HttpGet(this.url), worker)
            if (!assetIndexResponse.hasSuccess()) throw IllegalRequestResponseException("Failed to get game asset index: ${this.url}")

            val assetIndex = assetIndexResponse.get<GameAssetIndex>()
            FileUtils.writeStringToFile(assetIndexFile, MCSRLauncher.JSON.encodeToString(assetIndex), Charsets.UTF_8)
            GameAssetManager.updateChecksum(this.url, this.sha1)
            return assetIndex
        }
        return MCSRLauncher.JSON.decodeFromString(FileUtils.readFileToString(assetIndexFile, Charsets.UTF_8))
    }

    fun downloadAll(worker: LauncherWorker) {
        val beforeSum = GameAssetManager.getChecksum(this.url)
        val assetIndex = this.getAssetIndexes(worker)
        val shouldFastCheck = beforeSum == GameAssetManager.getChecksum(this.url)

        worker.setState("Downloading game assets...")
        var installedSize = 0L
        var installedFiles = 0
        for ((name, obj) in assetIndex.objects) {
//            worker.setState("Downloading game assets... | [ $installedFiles / ${assetIndex.objects.size} ]", false)
            worker.setSubText("Downloading: $name")
            val hash = obj.hash
            val subDir = hash.substring(0, 2)
            val outFile = GameAssetManager.OBJECTS_PATH.resolve("$subDir/$hash").toFile()
            if (!outFile.exists() || !(shouldFastCheck || (outFile.length() == obj.size && AssetUtils.compareHash(outFile, obj.hash)))) {
                FileDownloader.download("https://resources.download.minecraft.net/$subDir/$hash", outFile)
            }
            installedSize += obj.size
            installedFiles++
            worker.setProgress(installedSize / totalSize.toFloat())
        }
        worker.setSubText(null)
    }

}