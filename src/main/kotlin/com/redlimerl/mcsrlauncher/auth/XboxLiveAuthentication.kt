package com.redlimerl.mcsrlauncher.auth

import com.redlimerl.mcsrlauncher.exception.IllegalRequestResponseException
import com.redlimerl.mcsrlauncher.util.HttpUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity

class XboxLiveAuthentication {
    companion object {
        const val USER_TOKEN_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_TOKEN_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    }
}

@Serializable
data class XBLTokenReceiverAuth(
    val token: String,
    val hash: String
) {
    companion object {
        private fun createUserProperties(accessToken: String): JsonObject {
            return JsonObject(mapOf(
                Pair("Properties", JsonObject(mapOf(
                    Pair("AuthMethod", JsonPrimitive("RPS")),
                    Pair("SiteName", JsonPrimitive("user.auth.xboxlive.com")),
                    Pair("RpsTicket", JsonPrimitive("d=$accessToken"))
                ))),
                Pair("RelyingParty", JsonPrimitive("http://auth.xboxlive.com")),
                Pair("TokenType", JsonPrimitive("JWT"))
            ))
        }

        private fun createXSTSProperties(xblToken: XBLTokenReceiverAuth): JsonObject {
            return JsonObject(mapOf(
                Pair("Properties", JsonObject(mapOf(
                    Pair("SandboxId", JsonPrimitive("RETAIL")),
                    Pair("UserTokens", JsonArray(listOf(JsonPrimitive(xblToken.token))))
                ))),
                Pair("RelyingParty", JsonPrimitive("rp://api.minecraftservices.com/")),
                Pair("TokenType", JsonPrimitive("JWT"))
            ))
        }

        private fun createTokenFromJson(json: JsonObject): XBLTokenReceiverAuth {
            val token = json["Token"]?.jsonPrimitive?.content
            val hash = json["DisplayClaims"]?.jsonObject?.get("xui")?.jsonArray?.get(0)?.jsonObject?.get("uhs")?.jsonPrimitive?.content

            if (token == null || hash == null) throw NullPointerException("XBL Token/Hash is null")
            return XBLTokenReceiverAuth(token, hash)
        }

        fun createUserToken(worker: LauncherWorker, accessToken: String): XBLTokenReceiverAuth {
            worker.setState("Generating XBL User token")

            val postRequest = HttpPost(XboxLiveAuthentication.USER_TOKEN_URL)
            postRequest.setHeader("Content-Type", "application/json")
            postRequest.setHeader("Accept", "application/json")
            postRequest.entity = StringEntity(createUserProperties(accessToken).toString(), ContentType.APPLICATION_JSON)

            val response = HttpUtils.makeJsonRequest(postRequest, worker)

            if (!response.hasSuccess()) throw IllegalRequestResponseException("Failed to get Xbox-Live User Token")
            return createTokenFromJson(response.result!!.jsonObject)
        }

        fun createXSTSToken(worker: LauncherWorker, xblToken: XBLTokenReceiverAuth): XBLTokenReceiverAuth {
            worker.setState("Generating XBL XSTS token")

            val postRequest = HttpPost(XboxLiveAuthentication.XSTS_TOKEN_URL)
            postRequest.setHeader("Content-Type", "application/json")
            postRequest.setHeader("Accept", "application/json")
            postRequest.entity = StringEntity(createXSTSProperties(xblToken).toString(), ContentType.APPLICATION_JSON)

            val response = HttpUtils.makeJsonRequest(postRequest, worker)

            if (!response.hasSuccess()) throw IllegalRequestResponseException("Failed to get Xbox-Live XSTS Token")
            return createTokenFromJson(response.result!!.jsonObject)
        }
    }
}

@Serializable
data class XBLAuthenticationError(
    @SerialName("Identity") val identity: String,
    @SerialName("XErr") val error: Long,
    @SerialName("Message") val message: String,
    @SerialName("Redirect") val redirectUrl: String
) {
    fun getErrorMessage(): String {
        return when(error) {
            2148916227 -> "The account is banned from Xbox."
            2148916233 -> "The account doesn't have an Xbox account."
            2148916235 -> "The account is from a country where Xbox Live is not available/banned"
            2148916236 -> "The account needs adult verification on Xbox page."
            2148916237 -> "The account needs adult verification on Xbox page."
            2148916238 -> "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult. This only seems to occur when using a custom Microsoft Azure application. When using the Minecraft launchers client id, this doesn't trigger."
            else -> "Unknown"
        }
    }
}