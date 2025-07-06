package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.MCSRLauncher
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.*


object I18n {

    private val availableLanguages: Set<String> = setOfNotNull(
        "ko_KR"
    )
    private val resource = ResourceBundle.getBundle("lang/I18n", getLocale(), UTF8Control())

    init {
        MCSRLauncher.LOGGER.info("Loading language - {}", translate("lang"))
    }

    private fun getLocale(): Locale {
        if (!availableLanguages.contains(MCSRLauncher.options.language)) return Locale.ROOT
        val localeSplit = MCSRLauncher.options.language.split("_")
        return Locale(localeSplit[0], localeSplit[1])
    }

    fun translate(key: String, vararg args: Any): String {
        return String.format(if (resource.containsKey(key)) resource.getString(key) else key, *args)
    }

    class UTF8Control : ResourceBundle.Control() {
        override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle {
            val bundleName = toBundleName(baseName, locale)
            val resourceName = toResourceName(bundleName, "properties")
            var bundle: ResourceBundle? = null
            var stream: InputStream? = null
            if (reload) {
                val url: URL? = loader.getResource(resourceName)
                if (url != null) {
                    val connection: URLConnection = url.openConnection()
                    connection.setUseCaches(false)
                    stream = connection.getInputStream()
                }
            } else {
                stream = loader.getResourceAsStream(resourceName)
            }
            if (stream != null) {
                try {
                    val string = String(stream.readBytes(), Charsets.UTF_8)
                    bundle = PropertyResourceBundle(string.byteInputStream(Charsets.UTF_8))
                } finally {
                    stream.close()
                }
            }
            return bundle!!
        }
    }

}