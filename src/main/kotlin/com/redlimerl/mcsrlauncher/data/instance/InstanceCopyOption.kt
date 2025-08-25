package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import java.io.File

data class InstanceCopyOption(
    var worlds: Boolean = false,
    var gameOptions: Boolean = false,
    var mods: Boolean = false,
    var modConfigs: Boolean = false,
    var resourcePacks: Boolean = false,
    var playTime: Boolean = false
) {
    fun copyInstance(instanceName: String, instanceGroup: String?, baseInstance: BasicInstance) {
        val newInstance = InstanceManager.createInstance(instanceName, instanceGroup, baseInstance.minecraftVersion, baseInstance.lwjglVersion.copy(), baseInstance.fabricVersion?.copy(), baseInstance.mcsrRankedType)

        if (worlds) copyFiles(baseInstance, newInstance) { it.getGamePath().resolve("saves").toFile() }
        if (gameOptions) {
            val files = listOf("hotbar.nbt", "options.txt", "servers.dat", "servers.dat_old")
            for (file in files) {
                copyFiles(baseInstance, newInstance) { it.getGamePath().resolve(file).toFile() }
            }
        }
        if (mods) copyFiles(baseInstance, newInstance) { it.getModsPath().toFile() }
        if (modConfigs) {
            copyFiles(baseInstance, newInstance) { it.getGamePath().resolve("speedrunigt").toFile() }
            copyFiles(baseInstance, newInstance) { it.getGamePath().resolve("config").toFile() }
        }
        if (resourcePacks) {
            copyFiles(baseInstance, newInstance) { it.getGamePath().resolve("resourcepacks").toFile() }
            copyFiles(baseInstance, newInstance) { it.getGamePath().resolve("shaderpacks").toFile() }
            copyFiles(baseInstance, newInstance) { it.getGamePath().resolve("texturepacks").toFile() }
        }
        if (playTime) newInstance.playTime = baseInstance.playTime
        newInstance.save()
    }

    private fun copyFiles(baseInstance: BasicInstance, newInstance: BasicInstance, targetFile: (BasicInstance) -> File) {
        val baseFile = targetFile(baseInstance)
        val newFile = targetFile(newInstance)

        if (baseFile.exists()) {
            newFile.parentFile.mkdirs()
            if (baseFile.isDirectory) baseFile.copyRecursively(newFile)
            else baseFile.copyTo(newFile)
        }
    }
}