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

    @SerialName("org.graalvm.java")
    GRAALVM_JAVA("org.graalvm.java"),

    @SerialName("org.lwjgl")
    LWJGL2("org.lwjgl"),

    @SerialName("org.lwjgl3")
    LWJGL3("org.lwjgl3");

    companion object {
        val GAME_METAS = listOf(MINECRAFT, LWJGL2, LWJGL3)
        val FABRIC_METAS = listOf(FABRIC_LOADER, FABRIC_INTERMEDIARY)
        val JAVA_METAS = listOf(MOJANG_JAVA, ADOPTIUM_JAVA, AZUL_JAVA, GRAALVM_JAVA)
    }

}