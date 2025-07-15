package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MetaUniqueID(val value: String) {
    @SerialName("net.fabricmc.fabric-loader")
    FABRIC_LOADER("net.fabricmc.fabric-loader"),

    @SerialName("net.fabricmc.intermediary")
    FABRIC_INTERMEDIARY("net.fabricmc.intermediary"),

    @SerialName("net.minecraft")
    MINECRAFT("net.minecraft"),

    @SerialName("net.minecraft.java")
    MOJANG_JAVA("net.minecraft.java"),

    @SerialName("com.azul.java")
    AZUL_JAVA("com.azul.java"),

    @SerialName("net.adoptium.java")
    ADOPTIUM_JAVA("net.adoptium.java"),

    @SerialName("org.lwjgl")
    LWJGL2("org.lwjgl"),

    @SerialName("org.lwjgl3")
    LWJGL3("org.lwjgl3")

}