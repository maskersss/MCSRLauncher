package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.asset.java.JavaRuntimeManifest
import com.redlimerl.mcsrlauncher.data.device.DeviceOSType
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.instance.JavaContainer
import com.redlimerl.mcsrlauncher.launcher.GameAssetManager
import com.redlimerl.mcsrlauncher.network.FileDownloader
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import kotlin.io.path.name

object JavaUtils {

    fun javaExecutableName(): String {
        return if (DeviceOSType.WINDOWS.isOn()) "javaw.exe" else "java"
    }

    private fun toJavaExecutePath(path: Path): Path? {
        var basicPath = path
        if (basicPath.name != "bin") basicPath = basicPath.resolve("bin")
        var file = basicPath.resolve(javaExecutableName()).toFile()
        if (file.exists()) return file.toPath()

        val macPath = path.resolve("Contents")
        if (macPath.resolve("MacOS").toFile().exists()) {
            file = macPath.resolve("Home").resolve("bin").resolve(javaExecutableName()).toFile()
            if (file.exists()) return file.toPath()
        }

        return null
    }

    fun javaHomeVersions(): Set<JavaContainer> {
        val isWindows = DeviceOSType.WINDOWS.isOn()
        val containers = linkedSetOf<JavaContainer>()

        val javaHome = System.getenv("JAVA_HOME")
        if (!javaHome.isNullOrBlank()) {
            try {
                toJavaExecutePath(Paths.get(javaHome))?.let { containers.add(JavaContainer(it)) }
            } catch (e: Exception) {
                MCSRLauncher.LOGGER.error(e)
            }
        }

        val pathEnv = System.getenv("PATH")
        if (!pathEnv.isNullOrBlank()) {
            val paths = pathEnv.split(File.pathSeparator)
            for (dir in paths) {
                try {
                    toJavaExecutePath(Paths.get(dir))?.let { containers.add(JavaContainer(it)) }
                } catch (e: Exception) {
                    MCSRLauncher.LOGGER.error(e)
                }
            }
        }

        if (isWindows) {
            containers.addAll(findJavaFromWindowsRegistry())
        } else {
            containers.addAll(findJavaInStandardDirs())
        }

        try {
            toJavaExecutePath(Paths.get("../", "jre"))?.let { containers.add(JavaContainer(it)) }
        } catch (_: Throwable) {}
        try {
            toJavaExecutePath(Paths.get("", "jre"))?.let { containers.add(JavaContainer(it)) }
        } catch (_: Throwable) {}

        for (file in GameAssetManager.JAVA_PATH.toFile().listFiles()!!) {
            toJavaExecutePath(file.toPath())?.let { containers.add(JavaContainer(it)) }
        }

        for (customJavaPath in MCSRLauncher.options.customJavaPaths.toMutableList()) {
            toJavaExecutePath(Paths.get(customJavaPath))?.let { containers.add(JavaContainer(it)) }
                ?: {
                    MCSRLauncher.options.customJavaPaths.remove(customJavaPath)
                    MCSRLauncher.options.save()
                }
        }

        return containers
    }

    private fun findJavaInStandardDirs(): List<JavaContainer> {
        val result = mutableListOf<JavaContainer>()
        val stdDirs = listOf(
            "/usr/lib/jvm",
            "/usr/java",
            "/opt/java",
            "/opt/jdk",
            "/usr/bin",
            "/usr/local/bin"
        )

        for (dirPath in stdDirs) {
            val dir = File(dirPath)
            if (!dir.exists()) continue

            if (dir.isDirectory) {
                dir.walkTopDown()
                    .maxDepth(3)
                    .forEach { f ->
                        try {
                            if (f.isFile && f.name == "java" && f.canExecute()) result.add(JavaContainer(f.toPath()))
                        } catch (e: Exception) {
                            MCSRLauncher.LOGGER.error(e)
                        }
                    }
            } else if (dir.isFile && dir.name == "java" && dir.canExecute()) {
                try {
                    result.add(JavaContainer(dir.toPath()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return result.distinct()
    }

    private fun findJavaFromWindowsRegistry(): List<JavaContainer> {
        val result = mutableListOf<JavaContainer>()
        val regKeys = listOf(
            "HKLM\\SOFTWARE\\JavaSoft\\Java Development Kit",
            "HKLM\\SOFTWARE\\JavaSoft\\Java Runtime Environment"
        )

        for (key in regKeys) {
            try {
                val process = ProcessBuilder("reg", "query", key, "/s")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readLines()
                process.waitFor()

                for (line in output) {
                    if (line.trim().startsWith("JavaHome")) {
                        val parts = line.trim().split("    ")
                        if (parts.size >= 3) {
                            val javaHome = parts.last().trim()
                            val javaBin = Paths.get(javaHome, "bin", "java.exe").toFile()
                            if (javaBin.exists()) {
                                try {
                                    result.add(JavaContainer(javaBin.toPath()))
                                } catch (e: Exception) {
                                    MCSRLauncher.LOGGER.error(e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                MCSRLauncher.LOGGER.error(e)
            }
        }
        return result
    }

    fun extractJavaManifest(url: String, name: String, worker: LauncherWorker) {
        val targetDir = GameAssetManager.JAVA_PATH.resolve(name).toFile()
        if (targetDir.exists()) throw IllegalStateException("Directory is already exist: ${targetDir.absolutePath}")
        targetDir.mkdirs()

        val jsonRequest = MCSRLauncher.makeJsonRequest(HttpGet(url), worker)
        if (!jsonRequest.hasSuccess()) throw IllegalRequestResponseException("Failed to get java manifest: $url")

        val manifest = jsonRequest.get<JavaRuntimeManifest>()
        for ((relativePath, fileEntry) in manifest.files) {
            if (fileEntry.type != "file") continue
            val download = fileEntry.downloads["raw"] ?: continue
            val targetFile = targetDir.resolve(relativePath)

            worker.setState("Downloading $relativePath...")
            targetFile.parentFile.mkdirs()
            FileDownloader.download(download.url, targetFile)
        }
    }

    fun extractJavaArchive(url: String, name: String, worker: LauncherWorker) {
        val isZip = url.endsWith(".zip")
        val isTarGz = url.endsWith(".tar.gz")
        if (!isTarGz && !isZip) throw IllegalArgumentException("Unknown archive format: $url")

        val targetDir = GameAssetManager.JAVA_PATH.resolve(name).toFile()
        if (targetDir.exists()) throw IllegalStateException("Directory is already exist: ${targetDir.absolutePath}")
        targetDir.mkdirs()

        MCSRLauncher.LOGGER.info("Downloading: $url")
        val tempFile = Files.createTempFile("java-download-", if (isZip) ".zip" else ".tar.gz").toFile()
        FileDownloader.download(url, tempFile)
        tempFile.deleteOnExit()

        val rootDirs = getRootDirs(tempFile, isZip)

        worker.setState("Extracting Java...", false)
        MCSRLauncher.LOGGER.info("Extracting to: ${targetDir.absolutePath}")
        extractArchive(tempFile, targetDir, isZip, rootDirs.size == 1)
        MCSRLauncher.LOGGER.info("Extracted to: ${targetDir.absolutePath}")
    }

    private fun getRootDirs(file: File, isZip: Boolean): Set<String> {
        val roots = mutableSetOf<String>()
        fun <T : ArchiveEntry> checkEntry(archiveInput: ArchiveInputStream<T>) {
            var entry = archiveInput.nextEntry
            while (entry != null) {
                val root = entry.name.substringBefore("/")
                if (root.isNotEmpty()) roots.add(root)
                entry = archiveInput.nextEntry
            }
        }

        if (isZip) {
            ZipArchiveInputStream(FileInputStream(file)).use { checkEntry(it) }
        } else {
            GZIPInputStream(FileInputStream(file)).use { gzipIn ->
                TarArchiveInputStream(gzipIn).use { checkEntry(it) }
            }
        }
        return roots
    }

    private fun extractArchive(archiveFile: File, toFile: File, isZip: Boolean, isSingleRoot: Boolean) {
        fun <T : ArchiveEntry> checkEntry(archiveInput: ArchiveInputStream<T>) {
            var entry = archiveInput.nextEntry
            while (entry != null) {
                val entryName = entry.name

                val strippedName = if (isSingleRoot) {
                    entryName.substringAfter("/", missingDelimiterValue = entryName)
                } else entryName

                if (strippedName.isBlank()) {
                    entry = archiveInput.nextEntry
                    continue
                }

                val outFile = File(toFile, strippedName)

                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile.mkdirs()
                    FileOutputStream(outFile).use { archiveInput.copyTo(it) }
                }

                entry = archiveInput.nextEntry
            }
        }

        if (isZip) {
            ZipArchiveInputStream(FileInputStream(archiveFile)).use { checkEntry(it) }
        } else {
            GZIPInputStream(FileInputStream(archiveFile)).use { gzipIn ->
                TarArchiveInputStream(gzipIn).use { checkEntry(it) }
            }
        }
    }

}