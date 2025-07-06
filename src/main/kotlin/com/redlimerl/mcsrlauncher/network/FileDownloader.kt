package com.redlimerl.mcsrlauncher.network

import java.io.File
import java.net.URL
import kotlin.Exception

object FileDownloader {
    fun download(url: String, file: File) {
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        try {
            URL(url).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Throwable) {
            throw FileDownloadFailureException(url, file, e)
        }
    }
}

class FileDownloadFailureException(url: String, file: File, exception: Throwable) : Exception("Failed to download '${file.name}' from $url: ${exception.message}")