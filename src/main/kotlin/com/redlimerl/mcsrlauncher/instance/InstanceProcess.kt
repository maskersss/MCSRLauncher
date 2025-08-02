package com.redlimerl.mcsrlauncher.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.device.DeviceOSType
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.data.meta.LauncherTrait
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.FabricIntermediaryMetaFile
import com.redlimerl.mcsrlauncher.data.meta.file.FabricLoaderMetaFile
import com.redlimerl.mcsrlauncher.data.meta.file.LWJGLMetaFile
import com.redlimerl.mcsrlauncher.data.meta.file.MinecraftMetaFile
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.exception.InvalidAccessTokenException
import com.redlimerl.mcsrlauncher.launcher.AccountManager
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class InstanceProcess(val instance: BasicInstance) {

    var process: Process? = null
        private set

    @OptIn(DelicateCoroutinesApi::class)
    fun start(worker: LauncherWorker) {
        val javaTarget = instance.options.getSharedValue { it.javaPath }
        val noJavaException = IllegalStateException("Java has not selected. Try change your java path")
        if (javaTarget.isEmpty()) throw noJavaException
        val javaContainer: JavaContainer
        try {
            javaContainer = JavaContainer(Paths.get(javaTarget))
        } catch (e: Exception) {
            MCSRLauncher.LOGGER.error(e)
            throw noJavaException
        }

        MCSRLauncher.LOGGER.info("Loading Authentication: ${instance.name}")
        val activeAccount = AccountManager.getActiveAccount() ?: throw IllegalStateException("Active account is none")
        try {
            if (activeAccount.profile.checkTokenValidForLaunch(worker, activeAccount)) AccountManager.save()
        } catch (e: IllegalRequestResponseException) {
            throw InvalidAccessTokenException("Authentication Failed. Try remove and add your Minecraft account again.")
        }

        MCSRLauncher.LOGGER.info("Launching instance: ${instance.name}")
        instance.getGamePath().toFile().mkdirs()

        var mainClass: String
        val libraries = linkedSetOf<Path>()
        val libraryMap = arrayListOf<InstanceLibrary>()

        val arguments = arrayListOf(
            "-Xms${instance.options.getSharedValue { it.minMemory }}M",
            "-Xmx${instance.options.getSharedValue { it.maxMemory }}M"
        )

        arguments.addAll(instance.options.getSharedValue { it.jvmArguments }.split(" ").flatMap { it.split("\n") }.filter { it.isNotBlank() })

        val minecraftMetaFile = MetaManager.getVersionMeta<MinecraftMetaFile>(MetaUniqueID.MINECRAFT, instance.minecraftVersion)
            ?: throw IllegalStateException("${MetaUniqueID.MINECRAFT.value} version meta is not found")
        val minCompatibleVersion = minecraftMetaFile.compatibleJavaMajors.min()
        if (minCompatibleVersion > javaContainer.majorVersion) {
            throw IllegalStateException("Required minimum Java version is ${minCompatibleVersion}, you are at ${javaContainer.majorVersion}")
        }

        if (minecraftMetaFile.traits.contains(LauncherTrait.FIRST_THREAD_MACOS) && DeviceOSType.OSX.isOn()) {
            arguments.add("-XstartOnFirstThread")
        }

        minecraftMetaFile.libraries.filter { it.shouldApply() }.forEach { libraryMap.add(it.toInstanceLibrary()) }

        val mainJar = minecraftMetaFile.mainJar.getPath()
        mainClass = minecraftMetaFile.mainClass

        val gameArgs = minecraftMetaFile.minecraftArguments
            .replace("\${auth_player_name}", activeAccount.profile.nickname)
            .replace("\${version_name}", minecraftMetaFile.version)
            .replace("\${game_directory}", instance.getGamePath().absolutePathString())
            .replace("\${assets_root}", GameAssetManager.ASSETS_PATH.absolutePathString())
            .replace("\${game_assets}", instance.getGamePath().resolve("resources").absolutePathString())
            .replace("\${assets_index_name}", minecraftMetaFile.assetIndex.id)
            .replace("\${auth_uuid}", activeAccount.profile.uuid.toString())
            .replace("\${auth_access_token}", activeAccount.profile.accessToken ?: "")
            .replace("\${auth_session}", activeAccount.profile.accessToken ?: "")
            .replace("\${user_type}", "msa")
            .replace("\${version_type}", minecraftMetaFile.type.toTypeId())
            .replace("\${user_properties}", "{}")
            .split(" ")

        val lwjglMetaFile = MetaManager.getVersionMeta<LWJGLMetaFile>(instance.lwjglVersion.type, instance.lwjglVersion.version, worker)
            ?: throw IllegalStateException("LWJGL ${instance.lwjglVersion.version} is not found")
        lwjglMetaFile.libraries.filter { it.shouldApply() }.forEach { libraryMap.add(it.toInstanceLibrary()) }

        val fabric = instance.fabricVersion
        if (fabric != null) {
            val fabricLoaderMetaFile = MetaManager.getVersionMeta<FabricLoaderMetaFile>(MetaUniqueID.FABRIC_LOADER, fabric.loaderVersion)
                ?: throw IllegalStateException("${MetaUniqueID.FABRIC_LOADER.value} fabric loader version is not found")
            mainClass = fabricLoaderMetaFile.mainClass
            fabricLoaderMetaFile.libraries.forEach { libraryMap.add(it.toInstanceLibrary()) }

            val intermediaryMetaFile = MetaManager.getVersionMeta<FabricIntermediaryMetaFile>(MetaUniqueID.FABRIC_INTERMEDIARY, fabric.intermediaryVersion)
                ?: throw IllegalStateException("${fabric.intermediaryVersion} intermediary version is not found")
            libraryMap.add(intermediaryMetaFile.getLibrary(fabric.intermediaryType).toInstanceLibrary())
        }

        InstanceLibrary.fixLibraries(libraryMap)
        libraries.addAll(libraryMap.flatMap { it.paths })

        instance.getNativePath().toFile().mkdirs()
        FileUtils.deleteDirectory(instance.getNativePath().toFile())
        val nativeLibs = arrayListOf<Path>()
        for (libraryPath in libraries) {
            val libFile = libraryPath.toFile()
            if (!libFile.exists()) throw IllegalStateException("Library: ${libFile.name} is not exist!")
            if (libFile.name.endsWith(".jar") && libFile.name.contains("natives")) {
                if (MCSRLauncher.options.debug) MCSRLauncher.LOGGER.info("Native extracting: ${libFile.name}")
                AssetUtils.extractZip(libFile, instance.getNativePath().toFile(), true)
                nativeLibs.add(libraryPath)
            }
        }
        libraries.removeAll(nativeLibs.toSet())

        libraries.add(mainJar)

        val finalizeArgs = arrayListOf<String>()
        finalizeArgs.add(javaTarget)
        finalizeArgs.add("-Djava.library.path=${instance.getNativePath().absolutePathString()}")
        finalizeArgs.addAll(arguments)
        finalizeArgs.addAll(listOf("-cp", libraries.joinToString(File.pathSeparator) { it.absolutePathString() }))
        finalizeArgs.add(mainClass)
        finalizeArgs.addAll(gameArgs)

        if (MCSRLauncher.options.debug) MCSRLauncher.LOGGER.info(finalizeArgs)

        GlobalScope.launch {
            process = ProcessBuilder(finalizeArgs)
                .directory(instance.getGamePath().toFile())
//                .redirectOutput(OSUtils.getNullFile())
                .inheritIO()
                .start()
            MCSRLauncher.GAME_PROCESSES.add(this@InstanceProcess)
            instance.onLaunch()
            val exitCode = process!!.waitFor()
            Thread.sleep(2000L)
            onExit(exitCode)
        }
    }

    private fun onExit(code: Int) {
        MCSRLauncher.GAME_PROCESSES.remove(this)
        this.instance.onProcessExit(code)
    }

    fun exit() {
        process?.destroy()
    }

}