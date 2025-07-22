package com.redlimerl.mcsrlauncher.instance.mod

import com.redlimerl.mcsrlauncher.MCSRLauncher
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

interface ModData {
    val file: File
    val name: String
    val version: String
    var enabled: Boolean
        get() {
            return file.name.endsWith(".jar")
        }
        set(value) {
            file.renameTo(file.parentFile.resolve(file.nameWithoutExtension + ".${if (value) "jar" else "disabledjar"}"))
        }

    companion object {
        fun get(file: File): ModData? {
            if (!file.endsWith(".jar") && !file.endsWith(".diabledjar")) return null
            try {
                val jarFile = JarFile(file)

                val optifineEntry = jarFile.getJarEntry("Config.class")
                if (OptiFineData.OPTIFINE_PREFIX_REGEX.containsMatchIn(file.name) && optifineEntry != null) {
                    jarFile.close()
                    return OptiFineData(file)
                }

                val fabricJsonEntry = jarFile.getJarEntry("fabric.mod.json")
                if (fabricJsonEntry != null) {
                    val inputStream = jarFile.getInputStream(fabricJsonEntry)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val modJson = MCSRLauncher.JSON.decodeFromString<FabricModJson>(reader.readText())
                    reader.close()

                    return FabricModData(file, modJson.name, modJson.version)
                }

                jarFile.close()
            } catch (e: Throwable) {
                if (MCSRLauncher.options.debug) MCSRLauncher.LOGGER.error(e)
            }
            return null
        }
    }
}


class OptiFineData(override val file: File) : ModData {
    companion object {
        val OPTIFINE_PREFIX_REGEX = Regex("(?i)^OptiFine_")
    }

    override val name: String
        get() = "OptiFine"
    override val version: String
        get() = file.name.replace(OPTIFINE_PREFIX_REGEX, "").replace(".diabledjar", "").replace(".jar", "")
}

class FabricModData(override val file: File, override val name: String, override val version: String) : ModData