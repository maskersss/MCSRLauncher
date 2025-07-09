package com.redlimerl.mcsrlauncher.data

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.auth.MCTokenReceiverAuth
import com.redlimerl.mcsrlauncher.auth.MinecraftAuthentication
import com.redlimerl.mcsrlauncher.auth.XBLTokenReceiverAuth
import com.redlimerl.mcsrlauncher.data.serializer.UUIDSerializer
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.util.*

@Serializable
data class MinecraftProfile(
    @SerialName("id")
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,

    @SerialName("name")
    var nickname: String,

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

        val response = MCSRLauncher.makeJsonRequest(getRequest, worker)
        return response.hasSuccess()
    }

    fun checkTokenValidForLaunch(worker: LauncherWorker, microsoftAccount: MicrosoftAccount): Boolean {
        if (!this.shouldRefreshToken() && this.isAccessTokenValid(worker)) return true
        return this.refresh(worker, microsoftAccount, true)
    }
}