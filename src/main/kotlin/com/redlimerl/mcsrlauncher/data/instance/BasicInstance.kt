package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.*
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.exception.InvalidAccessTokenException
import com.redlimerl.mcsrlauncher.gui.UncloseableDialog
import com.redlimerl.mcsrlauncher.instance.InstanceProcess
import com.redlimerl.mcsrlauncher.launcher.AccountManager
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.OSUtils
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.nio.file.Path
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import kotlin.io.path.absolutePathString

@OptIn(DelicateCoroutinesApi::class)
@Serializable
data class BasicInstance(
    var name: String,
    var displayName: String,
    val indexes: HashMap<MetaUniqueID, String>,
    val options: InstanceOptions = InstanceOptions()
) {

    fun getDirPath(): Path {
        return MCSRLauncher.BASE_PATH.resolve("instances").resolve(name)
    }

    fun getGamePath(): Path {
        return this.getDirPath().resolve(".minecraft")
    }

    private fun getNativePath(): Path {
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
        for (index in indexes) {
            val indexVersions = MetaManager.getVersions(index.key, worker)
            val indexVersion = indexVersions.find { it.version == index.value }!!.getOrLoadMetaVersionFile<MetaVersionFile>(index.key, worker)
            indexVersion.install(worker)
        }
        MCSRLauncher.LOGGER.info("Installed every game files and libraries!")
    }

    fun launchInstance(worker: LauncherWorker) {
        if (this.isRunning()) return

        MCSRLauncher.LOGGER.info("Loading Authentication: $name")
        val activeAccount = AccountManager.getActiveAccount() ?: throw IllegalStateException("Active account is none")
        try {
            activeAccount.checkTokenValidForLaunch(worker)
        } catch (e: IllegalRequestResponseException) {
            throw InvalidAccessTokenException("Authentication Failed. Try remove and add your Minecraft account again.")
        }

        MCSRLauncher.LOGGER.info("Launching instance: $name")
        this.getGamePath().toFile().mkdirs()

        var mainClass: String
        val libraries = linkedSetOf<Path>()
        val arguments = arrayListOf(
            "-Xms${options.minMemory}M",
            "-Xmx${options.maxMemory}M"
        )

        val minecraftMetaFile = MetaManager.getVersionMeta<MinecraftMetaFile>(MetaUniqueID.MINECRAFT, indexes[MetaUniqueID.MINECRAFT])
            ?: throw IllegalStateException("${MetaUniqueID.MINECRAFT.value} version meta is not found")
        if (minecraftMetaFile.compatibleJavaMajors.min() > OSUtils.getJavaVersion()) {
            throw IllegalStateException("Required minimum Java version is ${minecraftMetaFile.compatibleJavaMajors.min()}, you are at ${OSUtils.getJavaVersion()}")
        }

        minecraftMetaFile.libraries.filter { it.shouldApply() }.forEach { libraries.addAll(it.getLibraryPaths()) }
        val mainJar = minecraftMetaFile.mainJar.getPath()
        mainClass = minecraftMetaFile.mainClass

        val gameArgs = minecraftMetaFile.minecraftArguments
            .replace("\${auth_player_name}", activeAccount.profile.nickname)
            .replace("\${version_name}", minecraftMetaFile.version)
            .replace("\${game_directory}", this.getGamePath().absolutePathString())
            .replace("\${assets_root}", GameAssetManager.ASSETS_PATH.absolutePathString())
            .replace("\${assets_index_name}", minecraftMetaFile.assetIndex.id)
            .replace("\${auth_uuid}", activeAccount.profile.uuid.toString())
            .replace("\${auth_access_token}", activeAccount.profile.accessToken ?: "")
            .replace("\${auto_session}", "")
            .replace("\${user_type}", "msa")
            .replace("\${version_type}", minecraftMetaFile.type.toTypeId())
            .replace("\${user_properties}", "{}")
            .split(" ")

        for (require in minecraftMetaFile.requires) {
            val lwjglMetaFile = MetaManager.getVersionMeta<LWJGLMetaFile>(require.uid, indexes[require.uid])
                ?: throw IllegalStateException("${require.uid.value} version meta is not found")
            lwjglMetaFile.libraries.filter { it.shouldApply() }.forEach { libraries.addAll(it.getLibraryPaths()) }
        }


        if (indexes.containsKey(MetaUniqueID.FABRIC_LOADER)) {
            val fabricLoaderMetaFile = MetaManager.getVersionMeta<FabricLoaderMetaFile>(MetaUniqueID.FABRIC_LOADER, indexes[MetaUniqueID.FABRIC_LOADER])
                ?: throw IllegalStateException("${MetaUniqueID.FABRIC_LOADER.value} version meta is not found")
            mainClass = fabricLoaderMetaFile.mainClass
            fabricLoaderMetaFile.libraries.forEach { libraries.add(it.getPath()) }

            for (require in fabricLoaderMetaFile.requires) {
                val intermediaryMetaFile = MetaManager.getVersionMeta<FabricIntermediaryMetaFile>(require.uid, indexes[require.uid])
                    ?: throw IllegalStateException("${require.uid.value} meta is not found")
                intermediaryMetaFile.libraries.forEach { libraries.add(it.getPath()) }
            }
        }

        this.getNativePath().toFile().mkdirs()
        val nativeLibs = arrayListOf<Path>()
        for (libraryPath in libraries) {
            val libFile = libraryPath.toFile()
            if (!libFile.exists()) throw IllegalStateException("Library: ${libFile.name} is not exist!")
            if (libFile.name.endsWith(".jar") && libFile.name.contains("natives")) {
                AssetUtils.extractZip(libFile, this.getNativePath().toFile())
                nativeLibs.add(libraryPath)
            }
        }
        libraries.removeAll(nativeLibs.toSet())

        libraries.add(mainJar)

        val finalizeArgs = arrayListOf<String>()
        finalizeArgs.add(options.javaPath.ifEmpty { "java" })
        finalizeArgs.add("-Djava.library.path=${this.getNativePath().absolutePathString()}")
        finalizeArgs.addAll(arguments)
        finalizeArgs.addAll(listOf("-cp", libraries.joinToString(File.pathSeparator) { it.absolutePathString() }))
        finalizeArgs.add(mainClass)
        finalizeArgs.addAll(gameArgs)

        if (MCSRLauncher.options.debug) MCSRLauncher.LOGGER.info(finalizeArgs)

        GlobalScope.launch {
            val process = ProcessBuilder(finalizeArgs)
                .directory(this@BasicInstance.getGamePath().toFile())
//                .inheritIO()
                .start()
            InstanceProcess(this@BasicInstance, process)
        }
    }

    fun launchWithDialog() {
        if (this.isRunning()) return
        object : LauncherWorker(MCSRLauncher.MAIN_FRAME, I18n.translate("launching_instance"), I18n.translate("loading") + "...") {
            override fun work(dialog: JDialog) {
                this.setState(I18n.translate("download_assets") + "...")
                install(this)
                this.setProgress(null)

                this.setState(I18n.translate("launching_instance") + "...")
                launchInstance(this)
            }
        }.start().showDialog()
    }

    fun getInstanceType(): String {
        return if (this.indexes.containsKey(MetaUniqueID.FABRIC_LOADER)) {
            "Fabric"
        } else if (this.indexes.containsKey(MetaUniqueID.MINECRAFT)) {
            "Vanilla"
        } else "Unknown"
    }

    fun getIconResource(): URL? {
        return if (this.indexes.containsKey(MetaUniqueID.FABRIC_LOADER)) {
            javaClass.getResource("/icons/fabric.png")
        } else if (this.indexes.containsKey(MetaUniqueID.MINECRAFT)) {
            javaClass.getResource("/icons/minecraft.png")
        } else javaClass.getResource("/icons/instance.png")
    }

    fun isRunning(): Boolean {
        return this.getProcess() != null
    }

    fun getProcess(): InstanceProcess? {
        return MCSRLauncher.GAME_PROCESSES.find { it.instance == this }
    }
}