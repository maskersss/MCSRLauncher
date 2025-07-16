package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.launcher.LauncherLanguage
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JDialog
import javax.swing.JFrame

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
        this.javaPathField.text = MCSRLauncher.options.javaPath

        this.javaChangeButton.addActionListener { JavaManagerGui(this@LauncherOptionGui) }
    }
}