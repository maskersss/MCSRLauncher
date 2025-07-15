package com.redlimerl.mcsrlauncher.instance

import com.github.zafarkhaja.semver.Version
import java.nio.file.Path

data class InstanceLibrary(
    private val artifact: String,
    val version: String,
    val paths: List<Path>
) {

    /**
     * Fix for ASM duplication between Minecraft vanilla & Fabric Loader
     * between `asm-all` and `asm`
     */
    fun getArtifactId(): String {
        val idx = artifact.lastIndexOf("-all")
        return if (idx >= 0) artifact.removeRange(idx, idx + "-all".length) else artifact
    }

    fun shouldReplaceFrom(other: InstanceLibrary): Boolean {
        if (this.getArtifactId() != other.getArtifactId()) return false
        if (Version.isValid(this.version) && Version.isValid(this.version)) {
            if (Version.parse(this.version).isHigherThan(Version.parse(other.version))) return true
        } else if (this.version != other.version) return true
        return false
    }
}