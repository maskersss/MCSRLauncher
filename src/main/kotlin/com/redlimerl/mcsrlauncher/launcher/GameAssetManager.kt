package com.redlimerl.mcsrlauncher.launcher

import com.redlimerl.mcsrlauncher.MCSRLauncher
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.io.FileUtils
import java.nio.file.Path

object GameAssetManager {

    private val CHECKSUM_PATH = MCSRLauncher.BASE_PATH.resolve("checksums.json")
    private val CHECKSUM = hashMapOf<String, String>()
    val ASSETS_PATH: Path = MCSRLauncher.BASE_PATH.resolve("assets")
    private val SKINS_PATH: Path = ASSETS_PATH.resolve("skins")
    val OBJECTS_PATH: Path = ASSETS_PATH.resolve("objects")
    val INDEXES_PATH: Path = ASSETS_PATH.resolve("indexes")
    val LIBRARIES_PATH: Path = MCSRLauncher.BASE_PATH.resolve("libraries")
    val JAVA_PATH: Path = MCSRLauncher.BASE_PATH.resolve("java")

    fun init() {
        ASSETS_PATH.toFile().mkdirs()
        OBJECTS_PATH.toFile().mkdirs()
        SKINS_PATH.toFile().mkdirs()
        INDEXES_PATH.toFile().mkdirs()
        LIBRARIES_PATH.toFile().mkdirs()
        JAVA_PATH.toFile().mkdirs()

        if (CHECKSUM_PATH.toFile().exists()) {
            MCSRLauncher.JSON.parseToJsonElement(FileUtils.readFileToString(CHECKSUM_PATH.toFile(), Charsets.UTF_8)).jsonObject.forEach {
                CHECKSUM[it.key] = it.value.jsonPrimitive.content
            }
        }
    }

    fun getChecksum(string: String): String {
        return CHECKSUM.getOrDefault(string, "")
    }

    fun updateChecksum(key: String, value: String) {
        CHECKSUM[key] = value
        FileUtils.writeStringToFile(CHECKSUM_PATH.toFile(), MCSRLauncher.JSON.encodeToString(CHECKSUM), Charsets.UTF_8)
    }
}