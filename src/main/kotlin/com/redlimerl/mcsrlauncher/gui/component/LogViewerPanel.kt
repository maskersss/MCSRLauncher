package com.redlimerl.mcsrlauncher.gui.component

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.gui.components.AbstractLogViewerPanel
import com.redlimerl.mcsrlauncher.util.HttpUtils.makeJsonRequest
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter

class LogViewerPanel(private val basePath: Path) : AbstractLogViewerPanel() {

    private var displayLiveLog = true
    private var autoScrollLive = true
    private var searchCount = 0

    init {
        layout = BorderLayout()
        add(this.rootPane, BorderLayout.CENTER)

        (liveLogArea.caret as DefaultCaret).updatePolicy = DefaultCaret.NEVER_UPDATE
        liveScrollPane.viewport.addChangeListener {
            val viewport = liveScrollPane.viewport
            val viewEnd = viewport.viewPosition.y + viewport.height
            val docHeight = liveLogArea.height

            autoScrollLive = (viewEnd >= (docHeight - (liveLogArea.font.size * 2)))
        }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = textChanged()
            override fun removeUpdate(e: DocumentEvent) = textChanged()
            override fun changedUpdate(e: DocumentEvent) = textChanged()

            fun textChanged() {
                searchCount = 0
            }
        })
        searchField.addActionListener {
            focusWord(getFocusedArea(), searchField.text)
        }
        findButton.addActionListener {
            focusWord(getFocusedArea(), searchField.text)
        }

        updateLogs()
        logFileBox.addActionListener {
            val selected = logFileBox.selectedItem as String
            if (selected == I18n.translate("text.process")) {
                (logCardPanel.layout as CardLayout).show(logCardPanel, "live")
                displayLiveLog = true
            } else {
                updateLogFile(selected)
                (logCardPanel.layout as CardLayout).show(logCardPanel, "file")
                displayLiveLog = false
            }
            searchCount = 0
        }

        reloadButton.addActionListener {
            if (!displayLiveLog) updateLogFile(logFileBox.selectedItem as String)
        }

        copyButton.addActionListener {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(getFocusedArea().text), null)
        }

        uploadButton.addActionListener {
            val selected = logFileBox.selectedItem as String
            val text = getFocusedArea().text
            if (text.isBlank()) return@addActionListener

            val result = JOptionPane.showConfirmDialog(this@LogViewerPanel, I18n.translate("message.upload_log_ask", selected), I18n.translate("text.warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
            if (result == JOptionPane.YES_OPTION) {
                val post = HttpPost("https://api.mclo.gs/1/log")
                post.entity = UrlEncodedFormEntity(listOf(BasicNameValuePair("content", text)))

                object : LauncherWorker() {
                    override fun work(dialog: JDialog) {
                        val request = makeJsonRequest(post, this)
                        if (request.hasSuccess()) {
                            val json = request.get<JsonObject>()
                            val url = json["url"]?.jsonPrimitive?.content ?: throw IllegalStateException("Unknown response: $json")
                            val arrayOption = arrayOf(I18n.translate("text.open.link"), I18n.translate("text.close"))
                            val openResult = JOptionPane.showOptionDialog(parent, I18n.translate("message.upload_log_success"), I18n.translate("text.upload"), JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, arrayOption, arrayOption[0])
                            if (openResult == 0) {
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null)
                                Desktop.getDesktop().browse(URI.create(url))
                            }
                        } else {
                            JOptionPane.showMessageDialog(this@LogViewerPanel, I18n.translate("message.upload_log_fail"), I18n.translate("text.error"), JOptionPane.ERROR_MESSAGE)
                        }
                    }
                }.start()
            }
        }
    }

    fun syncInstance(instance: BasicInstance) {
        instance.logViewerPanel = this
        instance.getProcess()?.syncLogViewer(this)
    }

    fun syncLauncher() {
        MCSRLauncher.LOG_APPENDER.syncLogViewer(this)
    }

    fun onLiveUpdate() {
        if (autoScrollLive) {
            liveLogArea.caretPosition = liveLogArea.document.length
        }
    }

    private fun getFocusedArea(): JTextArea {
        return if (displayLiveLog) liveLogArea else fileLogArea
    }

    private fun focusWord(textArea: JTextArea, word: String) {
        if (word.isBlank()) return

        val content = textArea.text
        val indexes = mutableListOf<Int>()
        var searchIndex = content.indexOf(word)
        while (searchIndex >= 0) {
            indexes.add(searchIndex)
            searchIndex = content.indexOf(word, searchIndex + word.length)
        }

        if (indexes.size <= searchCount) {
            searchCount = 0
        }

        if (indexes.isEmpty()) return

        val targetIndex = indexes[searchCount]
        textArea.caretPosition = targetIndex

        val highlighter = textArea.highlighter
        highlighter.removeAllHighlights()

        for (index in indexes) {
            highlighter.addHighlight(index, index + word.length, DefaultHighlighter.DefaultHighlightPainter(if (index == targetIndex) Color.ORANGE else Color.ORANGE.darker().darker()))
        }

        textArea.modelToView2D(targetIndex)?.let { textArea.scrollRectToVisible(it.bounds) }
        searchCount++
    }

    fun updateLogs() {
        logFileBox.addItem(I18n.translate("text.process"))

        val logsDir = basePath.resolve("logs").toFile()
        if (logsDir.exists() && logsDir.isDirectory) {
            for (file in logsDir.listFiles()!!.sortedByDescending { it.name }) {
                logFileBox.addItem("logs/" + file.name)
            }
        }

        val crashReportDir = basePath.resolve("crash-reports").toFile()
        if (crashReportDir.exists() && crashReportDir.isDirectory) {
            for (file in crashReportDir.listFiles()!!.sortedByDescending { it.name }) {
                logFileBox.addItem("crash-reports/" + file.name)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateLogFile(fileName: String) {
        val logFile = basePath.resolve(fileName).toFile()
        if (!logFile.exists()) return

        fileLogArea.text = I18n.translate("message.loading") + "..."
        GlobalScope.launch {
            val text = when (logFile.extension) {
                "gz" -> GZIPInputStream(FileInputStream(logFile)).use { gzip ->
                    InputStreamReader(gzip, Charsets.UTF_8).use { reader -> reader.readText() }
                }
                "txt", "log" -> logFile.readText()
                else -> ""
            }
            SwingUtilities.invokeLater {
                fileLogArea.text = text
                fileLogArea.caretPosition = 0
            }
        }
    }
}