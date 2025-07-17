package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.launcher.LauncherLanguage
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SwingUtils
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.SpinnerNumberModel
import kotlin.math.max
import kotlin.math.min

class LauncherOptionGui(parent: JFrame) : LauncherOptionDialog(parent) {

    init {
        title = I18n.translate("text.settings")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        this.cancelButton.addActionListener { this.dispose() }

        this.initLauncherTab()
        this.initJavaTab()

        I18n.translateGui(this)
        isVisible = true
    }

    private fun initLauncherTab() {
        this.launcherVersionLabel.text = I18n.translate("text.version.launcher") + ": v${MCSRLauncher.APP_VERSION}"

        for ((index, value) in LauncherLanguage.entries.withIndex()) {
            this.languageComboBox.addItem(value)
            if (MCSRLauncher.options.language == value) this.languageComboBox.selectedIndex = index
        }
        this.languageComboBox.addActionListener {
            val language = this.languageComboBox.selectedItem as LauncherLanguage?
            if (language != null) {
                MCSRLauncher.options.language = language
                MCSRLauncher.options.save()
            }
        }

        this.refreshMetaButton.addActionListener {
            object : LauncherWorker(this@LauncherOptionGui, I18n.translate("message.loading"), I18n.translate("message.updating.meta")) {
                override fun work(dialog: JDialog) = MetaManager.load(this, true)
            }.showDialog().start()
        }

        SwingUtils.fasterScroll(this.tabLauncherScrollPane)
    }

    private fun initJavaTab() {
        fun saveJavaPath() {
            MCSRLauncher.options.javaPath = this.javaPathField.text
            MCSRLauncher.options.save()
        }
        this.javaPathField.addActionListener { saveJavaPath() }
        this.javaPathField.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {}
            override fun focusLost(e: FocusEvent?) {
                saveJavaPath()
            }
        })

        fun refreshJavaPath() {
            this.javaPathField.text = MCSRLauncher.options.javaPath
        }

        refreshJavaPath()

        this.javaChangeButton.addActionListener {
            JavaManagerGui(this@LauncherOptionGui) {
                MCSRLauncher.options.javaPath = it
                MCSRLauncher.options.save()
                refreshJavaPath()
            }
        }


        this.javaMinMemorySlider.value = MCSRLauncher.options.minMemory
        this.javaMinMemorySpinner.model = SpinnerNumberModel(MCSRLauncher.options.minMemory, 512, 8096 * 2 + 1, 512)
        this.javaMaxMemorySlider.value = MCSRLauncher.options.maxMemory
        this.javaMaxMemorySpinner.model = SpinnerNumberModel(MCSRLauncher.options.maxMemory, 512, 8096 * 2 + 1, 512)
        this.javaMinMemorySlider.addChangeListener {
            this.javaMinMemorySlider.value = min(this.javaMinMemorySlider.value, MCSRLauncher.options.maxMemory)
            MCSRLauncher.options.minMemory = this.javaMinMemorySlider.value
            this.javaMinMemorySpinner.value = MCSRLauncher.options.minMemory
            MCSRLauncher.options.save()
        }
        this.javaMinMemorySpinner.addChangeListener {
            this.javaMinMemorySpinner.value = min(this.javaMinMemorySpinner.value as Int, MCSRLauncher.options.maxMemory)
            MCSRLauncher.options.minMemory = this.javaMinMemorySpinner.value as Int
            this.javaMinMemorySlider.value = MCSRLauncher.options.minMemory
            MCSRLauncher.options.save()
        }
        this.javaMaxMemorySlider.addChangeListener {
            this.javaMaxMemorySlider.value = max(this.javaMaxMemorySlider.value, MCSRLauncher.options.minMemory)
            MCSRLauncher.options.maxMemory = this.javaMaxMemorySlider.value
            this.javaMaxMemorySpinner.value = MCSRLauncher.options.maxMemory
            MCSRLauncher.options.save()
        }
        this.javaMaxMemorySpinner.addChangeListener {
            this.javaMaxMemorySpinner.value = max(this.javaMaxMemorySpinner.value as Int, MCSRLauncher.options.minMemory)
            MCSRLauncher.options.maxMemory = this.javaMaxMemorySpinner.value as Int
            this.javaMaxMemorySlider.value = MCSRLauncher.options.maxMemory
            MCSRLauncher.options.save()
        }


        this.jvmArgumentArea.text = MCSRLauncher.options.jvmArguments
        this.jvmArgumentArea.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {}
            override fun focusLost(e: FocusEvent?) {
                MCSRLauncher.options.jvmArguments = jvmArgumentArea.text
                MCSRLauncher.options.save()
            }
        })

        SwingUtils.fasterScroll(this.tabJavaScrollPane)
    }
}