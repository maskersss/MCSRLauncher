package com.redlimerl.mcsrlauncher.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.asset.GameAssetObject
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import org.apache.commons.io.FileUtils
import java.nio.file.Path

object LegacyLaunchFixer {

    fun assetFix(gameAssetObject: GameAssetObject, path: Path, worker: LauncherWorker) {
        worker.setState("Copying game assets to resources directory...")
        for ((name, obj) in gameAssetObject.getAssetIndexes(worker).objects) {
            val hash = obj.hash
            val subDir = hash.substring(0, 2)
            val sourceFile = GameAssetManager.OBJECTS_PATH.resolve("$subDir/$hash").toFile()
            val targetFile = path.resolve(name).toFile()

            if (!sourceFile.exists()) {
                MCSRLauncher.LOGGER.error("source file: $name is not exist! skip copying it.")
                continue
            }
            if (targetFile.exists()) continue

            targetFile.parentFile.mkdirs()
            FileUtils.copyFile(sourceFile, targetFile)
        }
    }

}