package com.redlimerl.mcsrlauncher

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont
import com.redlimerl.mcsrlauncher.gui.MainMenuGui
import com.redlimerl.mcsrlauncher.instance.InstanceProcess
import com.redlimerl.mcsrlauncher.launcher.*
import com.redlimerl.mcsrlauncher.network.JsonHttpClientResponseHandler
import com.redlimerl.mcsrlauncher.network.JsonResponseResult
import com.redlimerl.mcsrlauncher.network.JsonSha256HttpClientResponseHandler
import com.redlimerl.mcsrlauncher.network.JsonSha256ResponseResult
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JDialog
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.system.exitProcess

@OptIn(DelicateCoroutinesApi::class)
object MCSRLauncher {

    val APP_NAME: String = javaClass.simpleName
    val LOGGER: Logger = LogManager.getLogger(APP_NAME)
    val BASE_PATH: Path = Paths.get("").absolute().let { if (it.name != "launcher") it.resolve("launcher") else it }
    val IS_DEV_VERSION = javaClass.`package`.implementationVersion == null
    val APP_VERSION = javaClass.`package`.implementationVersion ?: "dev"
    val GAME_PROCESSES = arrayListOf<InstanceProcess>()
    val JSON = Json { ignoreUnknownKeys = true; prettyPrint = true }
    lateinit var MAIN_FRAME: MainMenuGui private set
    lateinit var options: LauncherOptions private set

    @JvmStatic
    fun main(args: Array<String>) {
        LOGGER.info("Starting launcher")

        // Setup Theme
        LOGGER.warn("Loading theme")
        FlatRobotoFont.install()
        FlatLaf.setPreferredFontFamily(FlatRobotoFont.FAMILY)
        FlatLaf.setPreferredLightFontFamily( FlatRobotoFont.FAMILY_LIGHT )
        FlatLaf.setPreferredSemiboldFontFamily( FlatRobotoFont.FAMILY_SEMIBOLD )
        FlatDarkLaf.setup()

        object : LauncherWorker(null, "Loading...") {
            override fun work(dialog: JDialog) {
                LOGGER.warn("Base Path: {}", BASE_PATH.absolutePathString())

                this.setState("Loading Launcher Options...")
                options = try {
                    JSON.decodeFromString<LauncherOptions>(FileUtils.readFileToString(LauncherOptions.path.toFile(), Charsets.UTF_8))
                } catch (e: NoSuchFileException)  {
                    LOGGER.warn("{} not found. creating new one", LauncherOptions.path.name)
                    LauncherOptions().also { it.save() }
                } catch (e: Exception) {
                    LOGGER.error(e, e)
                    LauncherOptions()
                }

                LOGGER.warn("Loading {}", AccountManager.path.name)
                this.setState("Loading Accounts...")
                AccountManager.load()

                LOGGER.warn("Loading {}", InstanceManager.path.name)
                this.setState("Loading Instances...")
                InstanceManager.load()

                LOGGER.warn("Loading Meta")
                this.setState("Loading Meta...")
                GameAssetManager.init()
                MetaManager.load(this)

                this.setState("UI Initializing...")
                dialog.dispose()

                LOGGER.warn("Setup gui")
                MAIN_FRAME = MainMenuGui()
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                exitProcess(1)
            }
        }.indeterminate().start().showDialog()
    }

    private val HTTP_CLIENT: CloseableHttpClient = HttpClientBuilder.create().build()

    fun makeJsonRequest(request: ClassicHttpRequest, worker: LauncherWorker): JsonResponseResult {
        if (options.debug) LOGGER.info("Requesting JSON to: " + request.uri.toString())
        return HTTP_CLIENT.execute(request, JsonHttpClientResponseHandler(worker))
    }

    fun makeJsonSha256Request(request: ClassicHttpRequest, worker: LauncherWorker): JsonSha256ResponseResult {
        if (options.debug) LOGGER.info("Requesting JSON(sha256) to: " + request.uri.toString())
        return HTTP_CLIENT.execute(request, JsonSha256HttpClientResponseHandler(worker))
    }
}
