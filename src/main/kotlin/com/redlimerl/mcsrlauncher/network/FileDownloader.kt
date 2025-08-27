package com.redlimerl.mcsrlauncher.network

import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.io.File
import java.net.URL

object FileDownloader {
    fun download(url: String, file: File, worker: LauncherWorker? = null, size: Long = 0) {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        try {
            val connection = URL(url).openConnection()
            val contentLength = connection.contentLengthLong.takeIf { it > 0 } ?: size

            connection.getInputStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (contentLength > 0) {
                            val progress = totalRead.toDouble() / contentLength
                            worker?.setProgress(progress.coerceIn(0.0, 1.0))
                        }
                    }
                    worker?.setProgress(null)
                }
            }
        } catch (e: Throwable) {
            throw FileDownloadFailureException(url, file, e)
        }
    }
}

class FileDownloadFailureException(url: String, file: File, exception: Throwable) : Exception("Failed to download '${file.name}' from $url: ${exception.message}")