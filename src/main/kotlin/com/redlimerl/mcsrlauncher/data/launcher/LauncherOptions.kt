package com.redlimerl.mcsrlauncher.data.launcher

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.util.JavaUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@Serializable
data class LauncherOptions(
    var debug: Boolean = false,
    var language: LauncherLanguage = LauncherLanguage.ENGLISH,
    var metaUrl: String = "https://mcsrlauncher.github.io/meta/",
    val customJavaPaths: LinkedHashSet<String> = linkedSetOf(),
    override var javaPath: String = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(JavaUtils.javaExecutableName()).absolutePathString(),
    override var jvmArguments: String = "",
    override var minMemory: Int = 512,
    override var maxMemory: Int = 2048
) : LauncherSharedOptions {

    companion object {
        val path: Path = MCSRLauncher.BASE_PATH.resolve("options.json")
    }

    fun save() {
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        FileUtils.writeStringToFile(path.toFile(), json.encodeToString(this), Charsets.UTF_8)
    }
}