package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.MCSRLauncher
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersionOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

object UpdaterUtils {

    private const val API_ENDPOINT = "https://api.github.com/repos/MCSRLauncher/Launcher/releases/latest"
    private const val JAR_NAME = "LauncherUpdater.jar"

    fun setup() {
        if (checkValidUpdater()) return

        val file = Paths.get("").resolve(JAR_NAME).toFile()

        val resourceStream: InputStream? = object {}.javaClass.getResourceAsStream("/$JAR_NAME")
        if (resourceStream == null) {
            MCSRLauncher.LOGGER.info("$JAR_NAME is not found in resources, probably you are on development environment.")
            return
        }

        MCSRLauncher.LOGGER.info("Copying updated/new $JAR_NAME from resources...")
        resourceStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun checkValidUpdater(): Boolean {
        val file = Paths.get("").resolve(JAR_NAME).toFile()
        if (!file.exists()) return false

        val resourceStream: InputStream = object {}.javaClass.getResourceAsStream("/$JAR_NAME") ?: return false
        val resourceChecksum = AssetUtils.calculateSha256(resourceStream)
        val localChecksum = AssetUtils.calculateSha256(Files.newInputStream(file.toPath()))

        return resourceChecksum == localChecksum
    }

    fun checkLatestVersion(worker: LauncherWorker): Version? {
        val current = MCSRLauncher.APP_VERSION.toVersionOrNull(false) ?: return null

        val request = HttpUtils.makeJsonRequest(HttpGet(API_ENDPOINT), worker)
        if (!request.hasSuccess()) return null

        val json = request.get<JsonObject>()
        val latest = Version.parse(json["tag_name"]!!.jsonPrimitive.content, false)
        if (current < latest) return latest
        return null
    }

    fun launchUpdater() {
        if (!checkValidUpdater()) return
        ProcessBuilder("java", "-jar", JAR_NAME).start()
        exitProcess(0)
    }
}