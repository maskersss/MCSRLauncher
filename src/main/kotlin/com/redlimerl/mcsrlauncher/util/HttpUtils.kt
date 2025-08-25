package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.MCSRLauncher.LOGGER
import com.redlimerl.mcsrlauncher.network.JsonHttpClientResponseHandler
import com.redlimerl.mcsrlauncher.network.JsonResponseResult
import com.redlimerl.mcsrlauncher.network.JsonSha256HttpClientResponseHandler
import com.redlimerl.mcsrlauncher.network.JsonSha256ResponseResult
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ClassicHttpRequest

object HttpUtils {
    private val HTTP_CLIENT: CloseableHttpClient = HttpClientBuilder.create().build()

    fun makeJsonRequest(request: ClassicHttpRequest, worker: LauncherWorker): JsonResponseResult {
        LOGGER.debug("Requesting JSON to: " + request.uri.toString())
        return HTTP_CLIENT.execute(request, JsonHttpClientResponseHandler(worker))
    }

    fun makeJsonSha256Request(request: ClassicHttpRequest, worker: LauncherWorker): JsonSha256ResponseResult {
        LOGGER.debug("Requesting JSON(sha256) to: " + request.uri.toString())
        return HTTP_CLIENT.execute(request, JsonSha256HttpClientResponseHandler(worker))
    }
}