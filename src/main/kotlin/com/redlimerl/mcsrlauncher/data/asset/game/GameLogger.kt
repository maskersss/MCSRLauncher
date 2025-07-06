package com.redlimerl.mcsrlauncher.data.asset.game

import com.redlimerl.mcsrlauncher.data.asset.FileAssetObject
import kotlinx.serialization.Serializable

@Serializable
data class GameLogger(
    val argument: String,
    val file: FileAssetObject,
    val type: String
)