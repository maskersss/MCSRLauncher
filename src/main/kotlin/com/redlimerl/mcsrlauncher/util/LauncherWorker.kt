package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.MCSRLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

abstract class LauncherWorker(
    private val parent: Component? = null,
    title: String = "",
    description: String = ""
) {

    companion object {
        fun empty(): LauncherWorker {
            return object : LauncherWorker() {
                override fun work(dialog: JDialog) {}
            }
        }
    }

    private var shouldShowDialog = false
    private var started = false
    private var cancelled = false
    private var currentJob: Job? = null
    val properties: HashMap<String, String> = hashMapOf()

    private val progressBar = JProgressBar(JProgressBar.HORIZONTAL, 0, 100)
    private val label = JLabel("", SwingConstants.CENTER).apply {
        font = font.let { it.deriveFont(it.size + 2f) }
    }
    private val subLabel = JLabel("", SwingConstants.CENTER).apply { isVisible = false }
    private val southPanel = JPanel(BorderLayout())
    val dialog = (if (parent is JFrame?) JDialog(parent, title, Dialog.ModalityType.MODELESS) else if (parent is JDialog) JDialog(parent, title, Dialog.ModalityType.MODELESS) else throw IllegalArgumentException("parent should be JFrame, JDialog or null")).apply {
        layout = BorderLayout()
        setSize(300, 150)
        setLocationRelativeTo(parent)
        MCSRLauncher.javaClass.classLoader?.getResource("icon.png")?.let {
            setIconImage(ImageIcon(it).image)
        }

        add(label, BorderLayout.CENTER)
        southPanel.add(subLabel, BorderLayout.NORTH)
        setProgress(null)
        southPanel.add(progressBar, BorderLayout.CENTER)
        add(southPanel, BorderLayout.SOUTH)

        defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {}
        })

        rootPane.registerKeyboardAction({}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)
        isResizable = false
    }

    init {
        setState(description)
    }


    protected abstract fun work(dialog: JDialog)

    open fun onError(e: Throwable) {
        val arrayOption = arrayOf(I18n.translate("text.copy.stacktrace_close"), I18n.translate("text.close"))
        val result = JOptionPane.showOptionDialog(parent, e.message, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, arrayOption, arrayOption[1])
        if (result == 0) {
            val selection = StringSelection(e.stackTraceToString())
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)

            JOptionPane.showMessageDialog(parent, "Stacktrace copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    fun start() {
        currentJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                if (shouldShowDialog) dialog.isVisible = true
                work(dialog)
                if (dialog.isVisible) dialog.dispose()
            } catch (e: Throwable) {
                MCSRLauncher.LOGGER.error(e.message, e)
                if (dialog.isVisible) dialog.dispose()
                onError(e)
            }
        }
        started = true
    }

    fun showDialog(): LauncherWorker {
        if (started) throw IllegalStateException("should be called before start()!")
        shouldShowDialog = true
        return this
    }

    fun cancelWork() {
        currentJob?.cancel()
        cancelled = true
    }

    fun isCancelled(): Boolean {
        return cancelled
    }

    fun addBottomPanel(component: JComponent) {
        this.southPanel.add(component, BorderLayout.SOUTH)
    }

    fun setState(string: String?, log: Boolean = true): LauncherWorker {
        if (!string.isNullOrBlank() && log) MCSRLauncher.LOGGER.info(string)
        label.text = string ?: ""
        refreshDialogWidth()
        return this
    }

    fun setSubText(string: String?): LauncherWorker {
        subLabel.text = string ?: ""
        if (!subLabel.isVisible && subLabel.text.isNotBlank()) refreshDialogWidth()
        return this
    }

    private fun refreshDialogWidth() {
        if (!started || !dialog.isVisible) return

        label.isVisible = label.text.isNotBlank()
        subLabel.isVisible = subLabel.text.isNotBlank()

        val oldSize = dialog.size
        val oldLocation = dialog.location
        val centerX = oldLocation.x + oldSize.width / 2
        val centerY = oldLocation.y + oldSize.height / 2

        val newWidth = (label.preferredSize.width + 60).coerceAtLeast(300)
        val newHeight = label.preferredSize.height + 90
        dialog.minimumSize = Dimension(dialog.minimumSize.width.coerceAtLeast(newWidth), newHeight)

        dialog.pack()

        val newSize = dialog.size
        val newX = centerX - newSize.width / 2
        val newY = centerY - newSize.height / 2
        dialog.setLocation(newX, newY)
    }

    fun setProgress(value: Float?): LauncherWorker {
        if (value == null) {
            progressBar.isVisible = false
        } else {
            progressBar.isVisible = true
            progressBar.value = (value * 100).toInt()
        }
        return this
    }

    fun indeterminate(): LauncherWorker {
        this.setProgress(1f)
        progressBar.isIndeterminate = true
        return this
    }
}