package com.redlimerl.mcsrlauncher.auth

import com.redlimerl.mcsrlauncher.data.MinecraftProfile
import com.redlimerl.mcsrlauncher.data.MinecraftProfileApiResponse
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.util.HttpUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity

class MinecraftAuthentication {
    companion object {
        const val LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val STORE_URL = "https://api.minecraftservices.com/entitlements/mcstore"
        const val PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
    }
}

@Serializable
data class MCTokenReceiverAuth(
    @SerialName("access_token") val token: String,
    @SerialName("expires_in") val expires: Int
) {
    companion object {
        fun create(worker: LauncherWorker, xblToken: XBLTokenReceiverAuth): MCTokenReceiverAuth {
            worker.setState("Generating MC token")

            val postRequest = HttpPost(MinecraftAuthentication.LOGIN_URL)
            postRequest.setHeader("Content-Type", "application/json")
            postRequest.setHeader("Accept", "application/json")
            postRequest.entity = StringEntity("""{"identityToken": "XBL3.0 x=${xblToken.hash};${xblToken.token}"}""", ContentType.APPLICATION_JSON)

            val response = HttpUtils.makeJsonRequest(postRequest, worker)

            if (!response.hasSuccess()) throw IllegalRequestResponseException("Failed to generate Minecraft Access Token")
            return response.get<MCTokenReceiverAuth>()
        }
    }

    fun checkOwnership(worker: LauncherWorker) {
        val getRequest = HttpGet(MinecraftAuthentication.STORE_URL)
        getRequest.setHeader("Authorization", "Bearer $token")
        getRequest.setHeader("Accept", "application/json")

        val response = HttpUtils.makeJsonRequest(getRequest, worker)

        if (!response.hasSuccess()) throw IllegalRequestResponseException("Failed to check game ownership")
        if (response.result!!.jsonObject["items"]!!.jsonArray.isEmpty()) throw IllegalStateException("You are not owned the game")
    }

    fun getProfile(worker: LauncherWorker): MinecraftProfile {
        val getRequest = HttpGet(MinecraftAuthentication.PROFILE_URL)
        getRequest.setHeader("Authorization", "Bearer $token")
        getRequest.setHeader("Accept", "application/json")

        val response = HttpUtils.makeJsonRequest(getRequest, worker)

        if (!response.hasSuccess()) throw IllegalRequestResponseException("Failed to check Minecraft profile")
        return response.get<MinecraftProfileApiResponse>().toProfile()
    }
}

@Serializable
data class MCAuthenticationError(
    val path: String,
    val error: String? = null,
    val errorMessage: String? = null
)