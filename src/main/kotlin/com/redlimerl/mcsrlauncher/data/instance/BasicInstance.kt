package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.meta.LauncherTrait
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.MetaVersionFile
import com.redlimerl.mcsrlauncher.data.meta.file.MinecraftMetaFile
import com.redlimerl.mcsrlauncher.data.meta.file.SpeedrunModsMetaFile
import com.redlimerl.mcsrlauncher.data.meta.mod.SpeedrunModMeta
import com.redlimerl.mcsrlauncher.data.meta.mod.SpeedrunModTrait
import com.redlimerl.mcsrlauncher.data.meta.mod.SpeedrunModVersion
import com.redlimerl.mcsrlauncher.gui.InstanceOptionGui
import com.redlimerl.mcsrlauncher.gui.component.LogViewerPanel
import com.redlimerl.mcsrlauncher.instance.InstanceProcess
import com.redlimerl.mcsrlauncher.instance.LegacyLaunchFixer
import com.redlimerl.mcsrlauncher.instance.mod.ModCategory
import com.redlimerl.mcsrlauncher.instance.mod.ModData
import com.redlimerl.mcsrlauncher.instance.mod.ModDownloadMethod
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.network.FileDownloader
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import io.github.z4kn4fein.semver.toVersion
import io.github.z4kn4fein.semver.toVersionOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
    val options: InstanceOptions = InstanceOptions(),

    @Transient
    var logViewerPanel: LogViewerPanel? = null,
    @Transient
    var optionDialog: InstanceOptionGui? = null
) {

    fun getInstancePath(): Path {
        return MCSRLauncher.BASE_PATH.resolve("instances").resolve(name)
    }

    fun getGamePath(): Path {
        return this.getInstancePath().resolve(".minecraft")
    }

    fun getModsPath(): Path {
        return this.getGamePath().resolve("mods")
    }

    fun getNativePath(): Path {
        return this.getInstancePath().resolve("native")
    }

    fun onCreate() {
        this.getInstancePath().toFile().mkdirs()
        this.getGamePath().toFile().mkdirs()
        this.getModsPath().toFile().mkdirs()
    }

    fun onLaunch() {
        MCSRLauncher.LOGGER.info("Launched instance: $name")
        InstanceManager.refreshInstanceList()
        logViewerPanel?.let { getProcess()?.syncLogViewer(it) }
        optionDialog?.setLauncherLaunched(true)
    }

    fun onProcessExit(code: Int) {
        MCSRLauncher.LOGGER.info("Exited instance: $name ($code)")
        FileUtils.deleteDirectory(this.getNativePath().toFile())
        InstanceManager.refreshInstanceList()
        optionDialog?.setLauncherLaunched(false)
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
                if (options.autoModUpdates) {
                    val updates = getSpeedRunModUpdates(this)
                    if (updates.isNotEmpty()) updateSpeedrunMods(this)
                }
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
        val beforeFile = this.getInstancePath().toFile()
        this.name = InstanceManager.getNewInstanceName(this.displayName)
        MCSRLauncher.LOGGER.info("Update instance name to $name from ${beforeFile.name}")
        val afterFile = this.getInstancePath().toFile()
        FileUtils.moveDirectory(beforeFile, afterFile)
    }

    fun getMods(): List<ModData> {
        val modsDir = this.getModsPath().toFile()
        if (!modsDir.exists() || !modsDir.isDirectory) return listOf()
        return modsDir.listFiles()!!.filter { it.isFile }.mapNotNull { ModData.get(it) }
    }

    fun installRecommendedSpeedrunMods(worker: LauncherWorker, modCategory: ModCategory, downloadMethod: ModDownloadMethod, accessibility: Boolean): List<ModData> {
        if (downloadMethod == ModDownloadMethod.UPDATE_EXISTING_MODS) return updateSpeedrunMods(worker)

        val list = arrayListOf<ModData>()
        this.getModsPath().toFile().mkdirs()
        this.fabricVersion ?: throw IllegalStateException("This instance does not have Fabric Loader")

        val modMeta = MetaManager.getVersionMeta<SpeedrunModsMetaFile>(MetaUniqueID.SPEEDRUN_MODS, SpeedrunModMeta.VERIFIED_MODS, worker) ?: throw IllegalStateException("Speedrun mods meta is not found")

        val installedMods = this.getMods()

        if (downloadMethod == ModDownloadMethod.DELETE_ALL_DOWNLOAD) {
            installedMods.forEach { it.delete() }
        }

        val availableMods = modMeta.mods.filter {
            it.recommended &&
                    it.isAvailable(this) &&
                    it.traits.all { trait ->
                        when (trait) {
                            SpeedrunModTrait.RSG -> modCategory == ModCategory.RANDOM_SEED
                            SpeedrunModTrait.SSG -> modCategory == ModCategory.SET_SEED
                            SpeedrunModTrait.ACCESSIBILITY -> accessibility
                            else -> true
                        }
                    }
        }

        for (mod in availableMods) {
            val version = mod.versions.find { it.isAvailableVersion(this) }!!

            worker.setState("Downloading ${mod.name} v${version.version}...")
            val file = this.getModsPath().resolve(version.url.split("/").last()).toFile()
            FileDownloader.download(version.url, file)
            installedMods.find { it.id == mod.modId }?.delete()
            list.add(ModData.get(file)!!)
        }
        return list
    }

    fun getSpeedRunModUpdates(worker: LauncherWorker): List<Pair<SpeedrunModMeta, SpeedrunModVersion>> {
        val list = arrayListOf<Pair<SpeedrunModMeta, SpeedrunModVersion>>()
        this.getModsPath().toFile().mkdirs()
        this.fabricVersion ?: throw IllegalStateException("This instance does not have Fabric Loader")

        val modMeta = MetaManager.getVersionMeta<SpeedrunModsMetaFile>(MetaUniqueID.SPEEDRUN_MODS, SpeedrunModMeta.VERIFIED_MODS, worker) ?: throw IllegalStateException("Speedrun mods meta is not found")

        val installedMods = this.getMods()
        for (mod in modMeta.mods.filter { it.isAvailable(this) }) {
            val version = mod.versions.find { it.isAvailableVersion(this) }!!

            val canUpdate = installedMods.any {
                it.id == mod.modId &&
                        if (it.version.toVersionOrNull(false) == null) {
                            it.version != version.version
                        } else {
                            it.version.toVersion(false) < version.version.toVersion(false)
                        }
            }
            if (canUpdate) list.add(mod to version)
        }
        return list
    }

    fun updateSpeedrunMods(worker: LauncherWorker): List<ModData> {
        val updates = getSpeedRunModUpdates(worker)

        val list = arrayListOf<ModData>()
        val installedMods = this.getMods()
        updates.forEach { (mod, version) ->
            worker.setState("Downloading ${mod.name} v${version.version}...")
            val file = this.getModsPath().resolve(version.url.split("/").last()).toFile()
            FileDownloader.download(version.url, file)
            installedMods.find { it.id == mod.modId }?.delete()
            list.add(ModData.get(file)!!)
        }

        return list
    }
}