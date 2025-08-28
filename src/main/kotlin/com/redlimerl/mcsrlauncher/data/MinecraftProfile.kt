package com.redlimerl.mcsrlauncher.data

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.auth.MCTokenReceiverAuth
import com.redlimerl.mcsrlauncher.auth.MinecraftAuthentication
import com.redlimerl.mcsrlauncher.auth.XBLTokenReceiverAuth
import com.redlimerl.mcsrlauncher.data.serializer.UUIDSerializer
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.HttpUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

@Serializable
data class MinecraftProfile(
    @SerialName("id")
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,

    @SerialName("name")
    var nickname: String,

    var skin: MinecraftSkin? = null,

    var activeCape: String? = null,
    var capes: ArrayList<MinecraftCape> = arrayListOf(),

    var accessToken: String? = null,

    private var expireAt: Long? = null
) {
    private fun shouldRefreshToken(): Boolean {
        return accessToken == null || expireAt == null || expireAt!! - 5 * 60 * 1000 < System.currentTimeMillis()
    }

    fun refresh(worker: LauncherWorker, microsoftAccount: MicrosoftAccount, force: Boolean = false): Boolean {
        if (!shouldRefreshToken() && !force) return false
        if (!microsoftAccount.refreshToken(worker, force)) return false
        worker.setState("Refreshing MC access token")

        val xboxUserToken = XBLTokenReceiverAuth.createUserToken(worker, microsoftAccount.accessToken)
        val xboxXSTSToken = XBLTokenReceiverAuth.createXSTSToken(worker, xboxUserToken)

        val mcToken = MCTokenReceiverAuth.create(worker, xboxXSTSToken)

        this.refreshToken(mcToken)

        val profileInfo = mcToken.getProfile(worker)
        this.nickname = profileInfo.nickname
        this.skin = profileInfo.skin
        this.activeCape = profileInfo.activeCape
        this.capes = profileInfo.capes

        MCSRLauncher.LOGGER.info("Refreshed MC access token successfully")

        return true
    }

    fun refreshToken(mcToken: MCTokenReceiverAuth) {
        accessToken = mcToken.token
        expireAt = System.currentTimeMillis() + (mcToken.expires * 1000)
    }

    private fun isAccessTokenValid(worker: LauncherWorker): Boolean {
        val getRequest = HttpGet(MinecraftAuthentication.PROFILE_URL)
        getRequest.setHeader("Authorization", "Bearer $accessToken")

        val response = HttpUtils.makeJsonRequest(getRequest, worker)
        return response.hasSuccess()
    }

    fun checkTokenValidForLaunch(worker: LauncherWorker, microsoftAccount: MicrosoftAccount): Boolean {
        if (!this.shouldRefreshToken() && this.isAccessTokenValid(worker)) return true
        return this.refresh(worker, microsoftAccount, true)
    }
}

@Serializable
enum class MinecraftSkinType {
    CLASSIC, SLIM
}

@Serializable
data class MinecraftSkin(
    val id: String,
    val data: String,
    val url: String,
    val variant: MinecraftSkinType
)

@Serializable
data class MinecraftCape(
    val id: String,
    val url: String,
    val alias: String
)

@Serializable
data class MinecraftProfileApiResponse(
    val id: String,
    val name: String,
    val skins: List<ApiSkin>,
    val capes: List<ApiCape>
) {
    @Serializable
    data class ApiSkin(
        val id: String,
        val state: String,
        val url: String,
        val textureKey: String,
        val variant: MinecraftSkinType
    ) {
        internal fun toSkin(): MinecraftSkin? {
            try {
                val image = ImageIO.read(URL(this.url))

                val outputStream = ByteArrayOutputStream()
                ImageIO.write(image, "png", outputStream)
                val bytes = outputStream.toByteArray()

                return MinecraftSkin(this.id, Base64.getEncoder().encodeToString(bytes), this.url, this.variant)
            } catch (e: Throwable) {
                MCSRLauncher.LOGGER.error("Failed to get player skin", e)
                return null
            }
        }
    }

    @Serializable
    data class ApiCape(
        val id: String,
        val state: String,
        val url: String,
        val alias: String
    ) {
        internal fun toCape(): MinecraftCape {
            return MinecraftCape(id, url, alias)
        }
    }

    fun toProfile(): MinecraftProfile {
        val activeSkin = skins.find { it.state == "ACTIVE" }
        val activeCape = capes.find { it.state == "ACTIVE" }

        return MinecraftProfile(
            AssetUtils.parseUUID(this.id), this.name, activeSkin?.toSkin(), activeCape?.id,
            capes.map { it.toCape() }.toCollection(arrayListOf())
        )
    }
}