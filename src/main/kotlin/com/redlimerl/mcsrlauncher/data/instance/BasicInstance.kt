package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.meta.LauncherTrait
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.*
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.exception.InvalidAccessTokenException
import com.redlimerl.mcsrlauncher.instance.InstanceLibrary
import com.redlimerl.mcsrlauncher.instance.InstanceProcess
import com.redlimerl.mcsrlauncher.instance.JavaContainer
import com.redlimerl.mcsrlauncher.instance.LegacyLaunchFixer
import com.redlimerl.mcsrlauncher.launcher.AccountManager
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.OSUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.nio.file.Path
import javax.swing.JDialog
import kotlin.io.path.absolutePathString

@OptIn(DelicateCoroutinesApi::class)
@Serializable
data class BasicInstance(
    var name: String,
    var displayName: String,
    val minecraftVersion: String,
    val lwjglVersion: LWJGLVersionData,
    val fabricVersion: FabricVersionData?,
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
        if (this.fabricVersion != null) {
            (MetaManager.getVersionMeta<MetaVersionFile>(MetaUniqueID.FABRIC_LOADER, this.fabricVersion.loaderVersion, worker)
                ?: throw IllegalStateException("Fabric Loader version $minecraftVersion is not exist")).install(worker)
            worker.properties["intermediary-type"] = this.fabricVersion.intermediaryType.name
            (MetaManager.getVersionMeta<MetaVersionFile>(MetaUniqueID.FABRIC_INTERMEDIARY, this.fabricVersion.intermediaryVersion, worker)
                ?: throw IllegalStateException("Fabric Intermediary version ${fabricVersion.intermediaryType} for ${fabricVersion.intermediaryVersion} is not exist")).install(worker)
        }

        MCSRLauncher.LOGGER.info("Installed every game files and libraries!")
    }

    fun launchInstance(worker: LauncherWorker) {
        if (this.isRunning()) return

        val javaTarget = options.javaPath.ifEmpty { MCSRLauncher.options.javaPath }
        val noJavaException = IllegalStateException("Java has not selected. Try change your java path")
        if (javaTarget.isEmpty()) throw noJavaException
        try {
            if (JavaContainer.getVersionLists(javaTarget).isEmpty()) throw noJavaException
        } catch (e: Exception) {
            MCSRLauncher.LOGGER.error(e)
            throw noJavaException
        }

        MCSRLauncher.LOGGER.info("Loading Authentication: $name")
        val activeAccount = AccountManager.getActiveAccount() ?: throw IllegalStateException("Active account is none")
        try {
            if (activeAccount.profile.checkTokenValidForLaunch(worker, activeAccount)) AccountManager.save()
        } catch (e: IllegalRequestResponseException) {
            throw InvalidAccessTokenException("Authentication Failed. Try remove and add your Minecraft account again.")
        }

        MCSRLauncher.LOGGER.info("Launching instance: $name")
        this.getGamePath().toFile().mkdirs()

        var mainClass: String
        val libraries = linkedSetOf<Path>()
        val libraryMap = linkedMapOf<String, InstanceLibrary>()

        val arguments = arrayListOf(
            "-Xms${options.minMemory}M",
            "-Xmx${options.maxMemory}M"
        )

        arguments.addAll(MCSRLauncher.options.jvmArguments.split(" ").flatMap { it.split("\n") }.filter { it.isNotBlank() })
        arguments.addAll(options.jvmArguments.split(" ").flatMap { it.split("\n") }.filter { it.isNotBlank() })

        val minecraftMetaFile = MetaManager.getVersionMeta<MinecraftMetaFile>(MetaUniqueID.MINECRAFT, this.minecraftVersion)
            ?: throw IllegalStateException("${MetaUniqueID.MINECRAFT.value} version meta is not found")
        if (minecraftMetaFile.compatibleJavaMajors.min() > OSUtils.getJavaVersion()) {
            throw IllegalStateException("Required minimum Java version is ${minecraftMetaFile.compatibleJavaMajors.min()}, you are at ${OSUtils.getJavaVersion()}")
        }

        minecraftMetaFile.libraries.filter { it.shouldApply() }.forEach { cleanupLibraryPaths(libraryMap, it.toInstanceLibrary()) }

        val mainJar = minecraftMetaFile.mainJar.getPath()
        mainClass = minecraftMetaFile.mainClass

        val gameArgs = minecraftMetaFile.minecraftArguments
            .replace("\${auth_player_name}", activeAccount.profile.nickname)
            .replace("\${version_name}", minecraftMetaFile.version)
            .replace("\${game_directory}", this.getGamePath().absolutePathString())
            .replace("\${assets_root}", GameAssetManager.ASSETS_PATH.absolutePathString())
            .replace("\${game_assets}", this.getGamePath().resolve("resources").absolutePathString())
            .replace("\${assets_index_name}", minecraftMetaFile.assetIndex.id)
            .replace("\${auth_uuid}", activeAccount.profile.uuid.toString())
            .replace("\${auth_access_token}", activeAccount.profile.accessToken ?: "")
            .replace("\${auth_session}", activeAccount.profile.accessToken ?: "")
            .replace("\${user_type}", "msa")
            .replace("\${version_type}", minecraftMetaFile.type.toTypeId())
            .replace("\${user_properties}", "{}")
            .split(" ")

        val lwjglMetaFile = MetaManager.getVersionMeta<LWJGLMetaFile>(this.lwjglVersion.type, this.lwjglVersion.version, worker)
            ?: throw IllegalStateException("LWJGL ${this.lwjglVersion.version} is not found")
        lwjglMetaFile.libraries.filter { it.shouldApply() }.forEach { cleanupLibraryPaths(libraryMap, it.toInstanceLibrary()) }


        if (this.fabricVersion != null) {
            val fabricLoaderMetaFile = MetaManager.getVersionMeta<FabricLoaderMetaFile>(MetaUniqueID.FABRIC_LOADER, this.fabricVersion.loaderVersion)
                ?: throw IllegalStateException("${MetaUniqueID.FABRIC_LOADER.value} fabric loader version is not found")
            mainClass = fabricLoaderMetaFile.mainClass
            fabricLoaderMetaFile.libraries.forEach { cleanupLibraryPaths(libraryMap, it.toInstanceLibrary()) }

            val intermediaryMetaFile = MetaManager.getVersionMeta<FabricIntermediaryMetaFile>(MetaUniqueID.FABRIC_INTERMEDIARY, this.fabricVersion.intermediaryVersion)
                ?: throw IllegalStateException("${this.fabricVersion.intermediaryVersion} intermediary version is not found")
            cleanupLibraryPaths(libraryMap, intermediaryMetaFile.getLibrary(this.fabricVersion.intermediaryType).toInstanceLibrary())
        }

        libraries.addAll(libraryMap.values.flatMap { it.paths })

        this.getNativePath().toFile().mkdirs()
        FileUtils.deleteDirectory(this.getNativePath().toFile())
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
        finalizeArgs.add(javaTarget)
        finalizeArgs.add("-Djava.library.path=${this.getNativePath().absolutePathString()}")
        finalizeArgs.addAll(arguments)
        finalizeArgs.addAll(listOf("-cp", libraries.joinToString(File.pathSeparator) { it.absolutePathString() }))
        finalizeArgs.add(mainClass)
        finalizeArgs.addAll(gameArgs)

        if (MCSRLauncher.options.debug) MCSRLauncher.LOGGER.info(finalizeArgs)

        GlobalScope.launch {
            val process = ProcessBuilder(finalizeArgs)
                .directory(this@BasicInstance.getGamePath().toFile())
//                .redirectOutput(OSUtils.getNullFile())
                .inheritIO()
                .start()
            InstanceProcess(this@BasicInstance, process)
        }
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

    private fun cleanupLibraryPaths(libraryMap: HashMap<String, InstanceLibrary>, library: InstanceLibrary) {
        if (!libraryMap.containsKey(library.getArtifactId()) || library.shouldReplaceFrom(libraryMap[library.getArtifactId()]!!))
            libraryMap[library.getArtifactId()] = library
    }
}