package com.redlimerl.mcsrlauncher.data.asset

import com.redlimerl.mcsrlauncher.data.asset.game.GameLibrary
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class BasicAssetObject(
    val sha1: String,
    val size: Long,
    val url: String
) {

    fun getPathFrom(library: GameLibrary): Path {
        return library.getPath().parent.resolve(this.url.split("/").last())
    }

}