package com.redlimerl.mcsrlauncher.instance

import com.redlimerl.mcsrlauncher.util.JavaUtils
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class JavaContainer(val path: Path, version: String? = null, vendor: String? = null) {

    val vendor: String
    val version: String

    init {
        if (version == null || vendor == null) {
            val process = ProcessBuilder(this.getJavaRuntimePath(), "-version").redirectErrorStream(false).start()
            val stderrLines = process.errorStream.bufferedReader().readLines()
            process.waitFor()

            if (stderrLines.isEmpty()) throw IllegalStateException("No output from java -version")

            val versionLine = stderrLines.firstOrNull {
                it.contains("version") || it.contains("openjdk version")
            } ?: throw IllegalStateException("Cannot detect Java version")

            val versionRegex = Regex("\"([^\"]+)\"")
            val versionMatch = versionRegex.find(versionLine) ?: throw IllegalStateException("Cannot parse Java version from: $versionLine")
            this.version = versionMatch.groupValues[1]

            val vendorLine = stderrLines.getOrNull(1) ?: "Unknown Vendor"
            this.vendor = vendorLine.trim()
        } else {
            this.version = version
            this.vendor = vendor
        }
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
        return arrayOf(this.version, this.path.absolutePathString())
    }

}