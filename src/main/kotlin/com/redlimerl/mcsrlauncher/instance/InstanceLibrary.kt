package com.redlimerl.mcsrlauncher.instance

import com.github.zafarkhaja.semver.Version
import com.redlimerl.mcsrlauncher.MCSRLauncher
import java.nio.file.Path

data class InstanceLibrary(
    private val artifact: String,
    val version: String,
    val paths: List<Path>
) {

    companion object {
        fun fixLibraries(list: ArrayList<InstanceLibrary>) {
            // Remove `asm-all` for legacy versions with fabric loader
            if (list.any { it.artifact == "org.ow2.asm:asm" })
                list.find { it.artifact == "org.ow2.asm:asm-all" }?.let { list.remove(it) }

            val result = ArrayList<InstanceLibrary>()

            list.groupBy { it.artifact }.forEach { (_, group) ->
                if (group.size <= 1) {
                    result.addAll(group)
                    return@forEach
                }

                val versioned = group.mapNotNull { Version.tryParse(it.version, false).map { v -> it to v }.orElse(null) }

                MCSRLauncher.LOGGER.info("$versioned")
                if (versioned.isEmpty()) {
                    result.addAll(group)
                    return@forEach
                }

                val maxVersion = versioned.maxOf { it.second }
                val latest = versioned.filter { it.second == maxVersion }.map { it.first }
                result.addAll(latest)
            }
            list.clear()
            list.addAll(result)
        }
    }

}