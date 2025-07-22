package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.instance.mod.ModCategory
import com.redlimerl.mcsrlauncher.instance.mod.ModDownloadMethod
import com.redlimerl.mcsrlauncher.util.I18n
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.JOptionPane

class ManageSpeedrunModsGui(val parent: JDialog) : ManageSpeedrunModsDialog(parent) {

    init {
        title = I18n.translate("text.manage_speedrun_mods")
        minimumSize = Dimension(400, 200)
        setLocationRelativeTo(parent)

        ModCategory.entries.forEach { categoryComboBox.addItem(it) }
        ModDownloadMethod.entries.forEach { downloadTypeComboBox.addItem(it) }

        accessibilityModsCheckBox.addActionListener {
            if (accessibilityModsCheckBox.isSelected) {
                JOptionPane.showMessageDialog(this@ManageSpeedrunModsGui, I18n.translate("message.warning.include_accessibility_mods"), I18n.translate("text.warning"), JOptionPane.WARNING_MESSAGE)
            }
        }

        applyButton.addActionListener {
            TODO("HERE!!!")
        }

        cancelButton.addActionListener { this.dispose() }

        I18n.translateGui(this)
        isVisible = true
    }
}