package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.device.DeviceOSType
import com.redlimerl.mcsrlauncher.instance.JavaContainer
import java.io.File
import java.nio.file.Paths

object JavaUtils {

    fun javaExecutableName(): String {
        return if (DeviceOSType.WINDOWS.isOn()) "javaw.exe" else "java"
    }

    fun javaHomeVersions(): Set<JavaContainer> {
        val isWindows = DeviceOSType.WINDOWS.isOn()
        val containers = linkedSetOf<JavaContainer>()

        val javaHome = System.getenv("JAVA_HOME")
        if (!javaHome.isNullOrBlank()) {
            try {
                val javaBin = Paths.get(javaHome, "bin", javaExecutableName()).toFile()
                if (javaBin.exists()) containers.add(JavaContainer(javaBin.toPath()))
            } catch (e: Exception) {
                MCSRLauncher.LOGGER.error(e)
            }
        }

        val pathEnv = System.getenv("PATH")
        if (!pathEnv.isNullOrBlank()) {
            val paths = pathEnv.split(File.pathSeparator)
            for (dir in paths) {
                try {
                    val javaBin = File(dir, javaExecutableName())
                    if (javaBin.exists()) containers.add(JavaContainer(javaBin.toPath()))
                } catch (e: Exception) {
                    MCSRLauncher.LOGGER.error(e)
                }
            }
        }

        if (isWindows) {
            containers.addAll(findJavaFromWindowsRegistry())
        } else {
            containers.addAll(findJavaInStandardDirs())
            containers.addAll(findJavaFromUpdateAlternatives())
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

    private fun findJavaFromUpdateAlternatives(): List<JavaContainer> {
        val result = mutableListOf<JavaContainer>()
        try {
            val process = ProcessBuilder("update-alternatives", "--list", "java")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()

            for (line in output) {
                val f = File(line.trim())
                if (f.exists() && f.canExecute()) {
                    try {
                        result.add(JavaContainer(f.toPath()))
                    } catch (e: Exception) {
                        MCSRLauncher.LOGGER.error(e)
                    }
                }
            }
        } catch (e: Exception) {
            MCSRLauncher.LOGGER.error(e)
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
}