package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.instance.mod.ModCategory
import com.redlimerl.mcsrlauncher.instance.mod.ModDownloadMethod
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.JOptionPane

class ManageSpeedrunModsGui(val parent: JDialog, val instance: BasicInstance, isNew: Boolean, val updater: () -> Unit) : ManageSpeedrunModsDialog(parent) {

    init {
        title = I18n.translate("text.manage_speedrun_mods")
        minimumSize = Dimension(400, 200)
        setLocationRelativeTo(parent)

        ModCategory.entries.forEach { categoryComboBox.addItem(it) }
        if (isNew) {
            downloadTypeComboBox.addItem(ModDownloadMethod.DOWNLOAD_RECOMMENDS)
            downloadTypeComboBox.isEnabled = false
        } else {
            ModDownloadMethod.entries.forEach { downloadTypeComboBox.addItem(it) }
        }

        accessibilityModsCheckBox.addActionListener {
            if (accessibilityModsCheckBox.isSelected) {
                JOptionPane.showMessageDialog(this@ManageSpeedrunModsGui, I18n.translate("message.warning.include_accessibility_mods"), I18n.translate("text.warning"), JOptionPane.WARNING_MESSAGE)
            }
        }

        applyButton.addActionListener {
            object : LauncherWorker(this@ManageSpeedrunModsGui, I18n.translate("message.loading"), I18n.translate("text.download_assets").plus("...")) {
                override fun work(dialog: JDialog) {
                    instance.installRecommendedSpeedrunMods(this, categoryComboBox.getItemAt(categoryComboBox.selectedIndex), downloadTypeComboBox.getItemAt(downloadTypeComboBox.selectedIndex), accessibilityModsCheckBox.isSelected)
                    this@ManageSpeedrunModsGui.dispose()
                    updater()
                }

                override fun onError(e: Throwable) {
                    updater()
                }
            }.showDialog().start()
        }

        cancelButton.addActionListener { this.dispose() }

        I18n.translateGui(this)
        isVisible = true
    }
}