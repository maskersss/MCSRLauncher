package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.launcher.LauncherLanguage
import com.redlimerl.mcsrlauncher.gui.component.JavaSettingsPanel
import com.redlimerl.mcsrlauncher.launcher.AccountManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SwingUtils
import com.redlimerl.mcsrlauncher.util.UpdaterUtils
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JOptionPane

class LauncherOptionGui(parent: JFrame) : LauncherOptionDialog(parent) {

    init {
        title = I18n.translate("text.settings")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        this.cancelButton.addActionListener { this.dispose() }

        this.initLauncherTab()
        this.initJavaTab()
        this.initInterfaceTab()

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

        this.checkUpdateButton.addActionListener {
            object : LauncherWorker(this@LauncherOptionGui, I18n.translate("text.check_update"), I18n.translate("message.checking_updates")) {
                override fun work(dialog: JDialog) {
                    val latestVersion = UpdaterUtils.checkLatestVersion(this)
                    dialog.dispose()
                    if (latestVersion != null) {
                        val updateConfirm = JOptionPane.showConfirmDialog(this@LauncherOptionGui, I18n.translate("message.new_update_found").plus("\nCurrent: ${MCSRLauncher.APP_VERSION}\nNew: $latestVersion"), I18n.translate("text.check_update"), JOptionPane.YES_NO_OPTION)
                        if (updateConfirm == JOptionPane.YES_OPTION) {
                            UpdaterUtils.launchUpdater()
                        }
                    } else {
                        JOptionPane.showMessageDialog(this@LauncherOptionGui, I18n.translate("message.latest_version"))
                    }
                }
            }.showDialog().start()
        }

        SwingUtils.fasterScroll(this.tabLauncherScrollPane)
    }

    private fun initJavaTab() {
        val javaSettingsPanel = JavaSettingsPanel(this, MCSRLauncher.options, MCSRLauncher.options::save)
        this.tabJavaScrollPane.setViewportView(javaSettingsPanel)
        SwingUtils.fasterScroll(this.tabJavaScrollPane)
    }

    private fun initInterfaceTab() {
        skinHeadTypeComboBox.addItem("2D")
        skinHeadTypeComboBox.addItem("3D")
        skinHeadTypeComboBox.selectedIndex = if (MCSRLauncher.options.skinHead3d) 1 else 0
        skinHeadTypeComboBox.addActionListener {
            MCSRLauncher.options.skinHead3d = skinHeadTypeComboBox.selectedIndex != 0
            AccountManager.clearSkinHeadCache()
            MCSRLauncher.options.save()
        }
    }
}