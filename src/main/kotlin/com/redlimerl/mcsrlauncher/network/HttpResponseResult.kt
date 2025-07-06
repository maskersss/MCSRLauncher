package com.redlimerl.mcsrlauncher.network

import com.redlimerl.mcsrlauncher.MCSRLauncher
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.security.MessageDigest

open class HttpResponseResult<T>(val code: Int, val result: T?) {
    fun hasSuccess(): Boolean {
        return code in 200..299 && result != null
    }

    override fun toString(): String {
        return "${javaClass.simpleName}{code=${code},result=${result}}"
    }
}

class RawResponseResult(code: Int, result: String?) : HttpResponseResult<String>(code, result)

class JsonResponseResult(code: Int, result: JsonElement?) : HttpResponseResult<JsonElement>(code, result) {
    inline fun <reified T> get(): T {
        return if (result == null) throw NullPointerException("JSON result is null.") else MCSRLauncher.JSON.decodeFromJsonElement<T>(result)
    }
}

class JsonSha256ResponseResult(code: Int, result: String?) : HttpResponseResult<String>(code, result) {
    inline fun <reified T> get(): T {
        return if (result == null) throw NullPointerException("JSON result is null.") else MCSRLauncher.JSON.decodeFromString<T>(result)
    }

    fun getSha256(): String {
        if (result == null) throw NullPointerException("JSON result is null.")

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(result.encodeToByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}