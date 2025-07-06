package com.redlimerl.mcsrlauncher.util

import com.google.common.hash.Hashing
import com.google.common.io.Files
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

object AssetUtils {

    fun compareHash(file: File, sha1: String): Boolean {
        @Suppress("DEPRECATION")
        return Files.asByteSource(file).hash(Hashing.sha1()).toString() == sha1
    }

    fun libraryNameToPath(path: Path, name: String): Path {
        return path.resolve(getLibraryMavenPath(name).replace("/", File.separator))
    }

    fun getLibraryMavenPath(name: String): String {
        val parts = name.split(":")
        if (parts.size < 3) throw IllegalArgumentException("Invalid Maven name format")

        val (group, artifact, version) = parts
        val groupPath = group.replace('.', '/')
        var jarName = "$artifact-$version"
        if (parts.size > 3) jarName += "-" + parts.drop(3).joinToString("-")
        jarName += ".jar"

        return "$groupPath/$artifact/$version/$jarName"
    }

    fun extractZip(jar: File, destDir: File) {
        ZipFile(jar).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (!entry.isDirectory) {
                    val outFile = File(destDir, entry.name)
                    outFile.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

}