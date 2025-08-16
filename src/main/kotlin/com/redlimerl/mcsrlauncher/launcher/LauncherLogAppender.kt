package com.redlimerl.mcsrlauncher.launcher

import com.redlimerl.mcsrlauncher.gui.component.LogViewerPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import javax.swing.SwingUtilities

class LauncherLogAppender(private val layout: PatternLayout)
    : AbstractAppender("Appender", null, layout, false, Property.EMPTY_ARRAY) {

    private var logArchive = StringBuilder()
    private var logChannel = Channel<String>(Channel.UNLIMITED)
    private var viewerUpdater: Job? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun append(event: LogEvent?) {
        val msg = layout.toSerializable(event) ?: return
        GlobalScope.launch {
            logChannel.send(msg)
            print(msg)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun syncLogViewer(logViewer: LogViewerPanel) {
        viewerUpdater?.cancel()

        viewerUpdater = GlobalScope.launch {
            SwingUtilities.invokeLater {
                logViewer.liveLogArea.append(logArchive.toString())
            }
            for (line in logChannel) {
                SwingUtilities.invokeLater {
                    logViewer.liveLogArea.append(line)
                    logArchive.append(line)
                    logViewer.onLiveUpdate()
                }
            }
        }
    }
}