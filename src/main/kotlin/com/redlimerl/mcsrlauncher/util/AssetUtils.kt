package com.redlimerl.mcsrlauncher.util

import com.google.common.hash.Hashing
import com.google.common.io.Files
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipFile
import kotlin.math.ln
import kotlin.math.pow


object AssetUtils {

    fun compareHash(file: File, sha1: String): Boolean {
        @Suppress("DEPRECATION")
        return Files.asByteSource(file).hash(Hashing.sha1()).toString() == sha1
    }

    fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream.use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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

    fun extractZip(jar: File, destDir: File, flatten: Boolean = false) {
        ZipFile(jar).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val name = entry.name
                if (!entry.isDirectory) {
                    val outFile: File = if (flatten) {
                        File(destDir, Paths.get(name).fileName.toString())
                    } else {
                        File(destDir, entry.name)
                    }
                    outFile.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1]
        val size = bytes / 1024.0.pow(exp.toDouble())
        return String.format("%.1f %sB", size, unit)
    }

    fun parseUUID(uuid: String): UUID {
        return UUID.fromString(
            uuid.replace("-".toRegex(), "").replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        )
    }

}