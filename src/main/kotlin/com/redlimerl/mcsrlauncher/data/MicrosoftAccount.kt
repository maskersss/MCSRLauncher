package com.redlimerl.mcsrlauncher.data

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.auth.MSTokenReceiverAuth
import com.redlimerl.mcsrlauncher.auth.MicrosoftAuthentication
import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.util.HttpUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.core5.http.message.BasicNameValuePair

@Serializable
data class MicrosoftAccount(
    val profile: MinecraftProfile,
    var accessToken: String,
    private var refreshToken: String,
    private var expireAt: Long,
    private var lastRefresh: Long = System.currentTimeMillis()
) {

    fun getLastRefreshTime(): Long {
        return lastRefresh
    }

    private fun shouldRefreshToken(): Boolean {
        return expireAt - 5 * 60 * 1000 < System.currentTimeMillis()
    }

    fun refreshToken(worker: LauncherWorker, force: Boolean = false): Boolean {
        if (!shouldRefreshToken() && !force) return false
        worker.setState("Try to refresh MSA token...")

        val postRequest = HttpPost(MicrosoftAuthentication.TOKEN_ACCESS_URL)
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded")
        postRequest.setHeader("Accept", "application/json")
        postRequest.entity = UrlEncodedFormEntity(listOf(
            BasicNameValuePair("client_id", MicrosoftAuthentication.getAzureClientId()),
            BasicNameValuePair("grant_type", "refresh_token"),
            BasicNameValuePair("refresh_token", this.refreshToken),
            BasicNameValuePair("scope", MicrosoftAuthentication.TOKEN_SCOPE)
        ))

        val response = HttpUtils.makeJsonRequest(postRequest, worker)
        if (!response.hasSuccess()) throw IllegalRequestResponseException("Failed to refresh MSA token! (${response.code}) ${response.result?.jsonObject?.get("error_codes")?.jsonArray}")

        val authResult = response.get<MSTokenReceiverAuth>()

        this.accessToken = authResult.accessToken
        this.refreshToken = authResult.refreshToken
        this.lastRefresh = System.currentTimeMillis()
        this.expireAt = this.lastRefresh + (1000 * authResult.expires)

        MCSRLauncher.LOGGER.info("Refreshed MSA token successfully")

        return true
    }

}