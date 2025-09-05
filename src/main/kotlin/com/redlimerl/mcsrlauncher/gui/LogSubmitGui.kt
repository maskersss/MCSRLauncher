package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.util.HttpUtils.makeRawRequest
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.Window
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.swing.JDialog

class LogSubmitGui(window: Window) : LogSubmitDialog(window) {
    var targetUrl: String? = ""

    init {
        title = I18n.translate("text.log")
        minimumSize = Dimension(400, 240)
        setLocationRelativeTo(window)

        openButton.isEnabled = false
        openButton.addActionListener {
            targetUrl?.let {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null)
                Desktop.getDesktop().browse(URI.create(it))
            }
        }

        analyzeButton.isEnabled = false
        analyzeButton.addActionListener {
            if (targetUrl == null) return@addActionListener

            object : LauncherWorker() {
                override fun work(dialog: JDialog) {
                    analyzeButton.isEnabled = false
                    statusLabel.text = I18n.translate("text.analyzing")
                    val post = HttpPost("https://maskers.xyz/log-analysis/analyse")
                    post.entity = UrlEncodedFormEntity(listOf(BasicNameValuePair("loglink", targetUrl!!)), StandardCharsets.UTF_8)
                    val request = makeRawRequest(post, this)
                    if (request.hasSuccess()) {
                        analyzeContextLabel.contentType = "text/html"
                        val regex = Regex("<pre>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL)
                        val match = regex.find(request.result!!)
                        analyzeContextLabel.text = "<html>${match?.groups?.get(1)?.value?.replace("<code>", "[")?.replace("</code>", "]")}</html>"
                        minimumSize = Dimension(500, 300)
                    } else {
                        analyzeContextLabel.text += "\nFailed to analyze: ${request.code}"
                    }
                    statusLabel.text = I18n.translate("text.analyzed")
                }
            }.start()
        }

        cancelButton.addActionListener { this.dispose() }

        I18n.translateGui(this)
        analyzeButton.text += " (via Background Pingu)"
    }

    fun updateUrl(url: String, activeAnalyze: Boolean) {
       statusLabel.text = I18n.translate("text.uploaded")
       targetUrl = url
       analyzeContextLabel.text = url
       openButton.isEnabled = true
       analyzeButton.isEnabled = activeAnalyze
    }
}