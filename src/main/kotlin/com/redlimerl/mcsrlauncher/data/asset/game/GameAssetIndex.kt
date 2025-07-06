package com.redlimerl.mcsrlauncher.data.asset.game

import com.redlimerl.mcsrlauncher.data.asset.HashAssetObject
import kotlinx.serialization.Serializable

@Serializable
data class GameAssetIndex(
    val objects: Map<String, HashAssetObject>
)
