package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.meta.LauncherTrait
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.MetaVersionFile
import com.redlimerl.mcsrlauncher.data.meta.file.MinecraftMetaFile
import com.redlimerl.mcsrlauncher.instance.InstanceProcess
import com.redlimerl.mcsrlauncher.instance.LegacyLaunchFixer
import com.redlimerl.mcsrlauncher.instance.mod.ModData
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.Path
import javax.swing.JDialog

@Serializable
data class BasicInstance(
    var name: String,
    var displayName: String,
    var minecraftVersion: String,
    var lwjglVersion: LWJGLVersionData,
    var fabricVersion: FabricVersionData?,
    val options: InstanceOptions = InstanceOptions()
) {

    fun getDirPath(): Path {
        return MCSRLauncher.BASE_PATH.resolve("instances").resolve(name)
    }

    fun getGamePath(): Path {
        return this.getDirPath().resolve(".minecraft")
    }

    fun getModsPath(): Path {
        return this.getGamePath().resolve("mods")
    }

    fun getNativePath(): Path {
        return this.getDirPath().resolve("native")
    }

    fun onCreate() {
        this.getDirPath().toFile().mkdirs()
    }

    fun onLaunch() {
        MCSRLauncher.LOGGER.info("Launched instance: $name")
        InstanceManager.refreshInstanceList()
    }

    fun onProcessExit(code: Int) {
        MCSRLauncher.LOGGER.info("Exited instance: $name ($code)")
        FileUtils.deleteDirectory(this.getNativePath().toFile())
        InstanceManager.refreshInstanceList()
    }

    fun install(worker: LauncherWorker) {
        worker.setState("installing game files and libraries...")

        // Minecraft
        val minecraftMeta = MetaManager.getVersionMeta<MinecraftMetaFile>(MetaUniqueID.MINECRAFT, this.minecraftVersion, worker) ?: throw IllegalStateException("Minecraft version $minecraftVersion is not exist")
        minecraftMeta.install(worker)
        if (minecraftMeta.traits.contains(LauncherTrait.LEGACY_LAUNCH)) {
            LegacyLaunchFixer.assetFix(minecraftMeta.assetIndex, this.getGamePath().resolve("resources"), worker)
        }

        // LWJGL
        (MetaManager.getVersionMeta<MetaVersionFile>(this.lwjglVersion.type, this.lwjglVersion.version, worker)
            ?: throw IllegalStateException("LWJGL version $minecraftVersion is not exist")).install(worker)

        // Fabric Loader / Intermediary
        val fabric = this.fabricVersion
        if (fabric != null) {
            (MetaManager.getVersionMeta<MetaVersionFile>(MetaUniqueID.FABRIC_LOADER, fabric.loaderVersion, worker)
                ?: throw IllegalStateException("Fabric Loader version $minecraftVersion is not exist")).install(worker)
            worker.properties["intermediary-type"] = fabric.intermediaryType.name
            (MetaManager.getVersionMeta<MetaVersionFile>(MetaUniqueID.FABRIC_INTERMEDIARY, fabric.intermediaryVersion, worker)
                ?: throw IllegalStateException("Fabric Intermediary version ${fabric.intermediaryType} for ${fabric.intermediaryVersion} is not exist")).install(worker)
        }

        MCSRLauncher.LOGGER.info("Installed every game files and libraries!")
    }

    fun launchInstance(worker: LauncherWorker) {
        if (this.isRunning()) return

        InstanceProcess(this@BasicInstance).start(worker)
    }

    fun launchWithDialog() {
        if (this.isRunning()) return
        object : LauncherWorker(MCSRLauncher.MAIN_FRAME, I18n.translate("instance.launching"), I18n.translate("message.loading") + "...") {
            override fun work(dialog: JDialog) {
                this.setState(I18n.translate("text.download_assets") + "...")
                install(this)
                this.setProgress(null)

                this.setState(I18n.translate("instance.launching") + "...")
                launchInstance(this)
            }
        }.showDialog().start()
    }

    fun getInstanceType(): String {
        return if (this.fabricVersion != null) "Fabric" else "Vanilla"
    }

    fun getIconResource(): URL? {
        return if (this.fabricVersion != null)
            javaClass.getResource("/icons/fabric.png")
        else
            javaClass.getResource("/icons/minecraft.png")
    }

    fun isRunning(): Boolean {
        return this.getProcess() != null
    }

    fun getProcess(): InstanceProcess? {
        return MCSRLauncher.GAME_PROCESSES.find { it.instance == this }
    }

    fun setInstanceName(text: String) {
        this.displayName = text
        val beforeFile = this.getDirPath().toFile()
        this.name = InstanceManager.getNewInstanceName(this.displayName)
        MCSRLauncher.LOGGER.info("Update instance name to $name from ${beforeFile.name}")
        val afterFile = this.getDirPath().toFile()
        FileUtils.moveDirectory(beforeFile, afterFile)
    }

    fun getMods(): List<ModData> {
        val modsDir = this.getModsPath().toFile()
        if (!modsDir.exists() || !modsDir.isDirectory) return listOf()
        return modsDir.listFiles()!!.filter { it.isFile }.mapNotNull { ModData.get(it) }
    }
}