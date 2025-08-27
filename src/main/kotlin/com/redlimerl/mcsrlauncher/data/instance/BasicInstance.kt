package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.instance.mcsrranked.MCSRRankedPackType
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
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SpeedrunUtils
import io.github.z4kn4fein.semver.toVersion
import io.github.z4kn4fein.semver.toVersionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames
import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JDialog

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BasicInstance(
    @JsonNames("id", "name") var id: String,
    var displayName: String,
    var group: String = "",
    var minecraftVersion: String,
    var lwjglVersion: LWJGLVersionData,
    var fabricVersion: FabricVersionData?,
    var mcsrRankedType: MCSRRankedPackType? = null,
    var options: InstanceOptions = InstanceOptions(),
    var playTime: Long = 0,
    @Transient
    var logViewerPanel: LogViewerPanel? = null,
    @Transient
    var optionDialog: InstanceOptionGui? = null,
    @Transient
    var lastLaunch: Long = System.currentTimeMillis()
) {

    fun getInstancePath(): Path {
        return InstanceManager.INSTANCES_PATH.resolve(id)
    }

    fun getGamePath(): Path {
        return this.getInstancePath().resolve(".minecraft")
    }

    fun getModsPath(): Path {
        return this.getGamePath().resolve("mods")
    }

    fun getWorldsPath(): Path {
        return this.getGamePath().resolve("saves")
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
        MCSRLauncher.LOGGER.info("Launched instance: $id")
        InstanceManager.refreshInstanceList()
        logViewerPanel?.let { getProcess()?.syncLogViewer(it) }
        optionDialog?.setLauncherLaunched(true)
        lastLaunch = System.currentTimeMillis()
    }

    fun onProcessExit(code: Int) {
        MCSRLauncher.LOGGER.info("Exited instance: $id ($code)")
        FileUtils.deleteDirectory(this.getNativePath().toFile())
        InstanceManager.refreshInstanceList()
        optionDialog?.setLauncherLaunched(false)
        playTime += (System.currentTimeMillis() - lastLaunch) / 1000
        this.save()
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

        if (this.mcsrRankedType != null && fabric != null && this.minecraftVersion == "1.16.1") {
            SpeedrunUtils.getLatestMCSRRankedVersion(worker)?.download(this, worker)
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
                this.setState(I18n.translate("text.download.assets") + "...")
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
        return if (this.mcsrRankedType != null) javaClass.getResource("/icons/mcsrranked.png")
            else if (this.fabricVersion != null) javaClass.getResource("/icons/fabric.png")
            else javaClass.getResource("/icons/minecraft.png")
    }

    fun isRunning(): Boolean {
        return this.getProcess() != null
    }

    fun getProcess(): InstanceProcess? {
        return MCSRLauncher.GAME_PROCESSES.find { it.instance == this }
    }

    fun updateName(text: String) {
        this.displayName = text
        val beforeFile = this.getInstancePath().toFile()
        this.id = InstanceManager.getNewInstanceName(this.displayName)
        MCSRLauncher.LOGGER.info("Update instance name to $id from ${beforeFile.name}")
        val afterFile = this.getInstancePath().toFile()
        FileUtils.moveDirectory(beforeFile, afterFile)
    }

    fun getMods(): List<ModData> {
        val modsDir = this.getModsPath().toFile()
        if (!modsDir.exists() || !modsDir.isDirectory) return listOf()
        return modsDir.listFiles()!!.filter { it.isFile }.mapNotNull { ModData.get(it) }
    }

    fun installRecommendedSpeedrunMods(worker: LauncherWorker, modListVersion: String, modCategory: ModCategory, downloadMethod: ModDownloadMethod, accessibility: Boolean): List<ModData> {
        if (downloadMethod == ModDownloadMethod.UPDATE_EXISTING_MODS) return updateSpeedrunMods(worker)

        this.getModsPath().toFile().mkdirs()
        this.fabricVersion ?: throw IllegalStateException("This instance does not have Fabric Loader")

        val modMeta = MetaManager.getVersionMeta<SpeedrunModsMetaFile>(MetaUniqueID.SPEEDRUN_MODS, modListVersion, worker) ?: throw IllegalStateException("Speedrun mods meta is not found")

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

        val total = availableMods.size
        val completed = AtomicInteger(0)
        val modList = Collections.synchronizedList(mutableListOf<ModData>())

        AssetUtils.doConcurrency(availableMods) { mod ->
            val version = mod.versions.find { it.isAvailableVersion(this@BasicInstance) }!!
            worker.setState("Downloading ${mod.name} v${version.version}...")

            val file = getModsPath().resolve(version.url.split("/").last()).toFile()
            withContext(Dispatchers.IO) {
                FileDownloader.download(version.url, file)
            }

            installedMods.find { it.id == mod.modId }?.delete()
            modList.add(ModData.get(file)!!)

            val done = completed.incrementAndGet()
            worker.setProgress(done.toFloat() / total.toFloat())
        }

        return modList
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

    fun save() {
        val configJson = this.getInstancePath().resolve("instance.json")
        configJson.toFile().writeText(MCSRLauncher.JSON.encodeToString(this))
    }
}