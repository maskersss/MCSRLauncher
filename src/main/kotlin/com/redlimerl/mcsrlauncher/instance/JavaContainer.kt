package com.redlimerl.mcsrlauncher.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.util.JavaUtils
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

class JavaContainer(val path: Path, version: String? = null, vendor: String? = null) {

    val vendor: String
    val version: String
    val majorVersion: Int

    companion object {
        fun getVersionLists(javaPath: String): List<String> {
            val process = ProcessBuilder(javaPath, "-version").redirectErrorStream(false).start()
            val stderrLines = process.errorStream.bufferedReader().readLines()
            process.waitFor()

            if (stderrLines.isEmpty()) throw IllegalStateException("No output from java -version")

            stderrLines.firstOrNull {
                it.contains("version") || it.contains("openjdk version")
            } ?: throw IllegalStateException("Cannot detect Java version")

            return stderrLines
        }
    }

    init {
        var possibleVendor: String
        var possibleVersion: String
        if (version == null || vendor == null) {
            val stderrLines = getVersionLists(this.getJavaRuntimePath())

            val versionLine = stderrLines.firstOrNull {
                it.contains("version") || it.contains("openjdk version")
            } ?: throw IllegalStateException("Cannot detect Java version")

            val versionRegex = Regex("\"([^\"]+)\"")
            val versionMatch = versionRegex.find(versionLine) ?: throw IllegalStateException("Cannot parse Java version from: $versionLine")
            possibleVersion = versionMatch.groupValues[1]

            val vendorLine = stderrLines.getOrNull(1) ?: "Unknown Vendor"
            possibleVendor = vendorLine.trim()
        } else {
            possibleVendor = vendor
            possibleVersion = version
        }

        try {
            val properties = Properties()
            path.parent.parent.resolve("release").toFile().bufferedReader().use { reader -> properties.load(reader) }

            possibleVendor = properties.getProperty("IMPLEMENTOR")?.replace("\"", "") ?: possibleVendor
            possibleVersion = properties.getProperty("JAVA_VERSION")?.replace("\"", "") ?: possibleVersion
        } catch (e: Exception) {
            MCSRLauncher.LOGGER.error(e)
        }

        this.version = possibleVersion
        this.vendor = possibleVendor
        this.majorVersion = this.version.split(".").first().toInt().let { if (it == 1) this.version.split(".")[1].toInt() else it }
    }

    fun getJavaRuntimePath(): String {
        return path.parent.resolve(JavaUtils.javaExecutableName()).absolutePathString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is JavaContainer) {
            return this.version == other.version && this.vendor == other.vendor
        }
        return false
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + vendor.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    fun dataArray(): Array<String> {
        return arrayOf(this.version, this.vendor, this.path.absolutePathString())
    }

}