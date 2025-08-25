package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.data.instance.InstanceCopyOption
import com.redlimerl.mcsrlauncher.gui.component.InstanceGroupComboBox
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.awt.Dimension
import java.awt.Window
import javax.swing.JDialog
import javax.swing.JOptionPane

class CopyInstanceGui(parent: Window, private val instance: BasicInstance) : CopyInstanceDialog(parent) {

    init {
        title = I18n.translate("text.copy.instance")
        minimumSize = Dimension(360, 220)
        setLocationRelativeTo(parent)

        instanceNameField.text = "Copy of " + instance.displayName
        InstanceGroupComboBox.init(instanceGroupBox)
        instanceGroupBox.selectedItem = instance.group

        copyButton.addActionListener {
            if (instanceNameField.text.isBlank()) return@addActionListener

            val copyOption = InstanceCopyOption()
            copyOption.worlds = copyWorldsCheckBox.isSelected
            copyOption.gameOptions = copyGameOptionsCheckBox.isSelected
            copyOption.mods = copyModsCheckBox.isSelected
            copyOption.modConfigs = copyModConfigsCheckBox.isSelected
            copyOption.resourcePacks = copyResourcePacksCheckBox.isSelected
            copyOption.playTime = copyPlayTimeCheckBox.isSelected

            object : LauncherWorker(this@CopyInstanceGui, I18n.translate("text.copy.instance"), I18n.translate("message.loading")) {
                override fun work(dialog: JDialog) {
                    this@CopyInstanceGui.dispose()
                    copyOption.copyInstance(instanceNameField.text, instanceGroupBox.selectedItem?.toString(), instance)
                    dialog.dispose()
                    JOptionPane.showMessageDialog(this@CopyInstanceGui, I18n.translate("message.copy_instance.success"))
                }
            }.showDialog().start()
        }

        cancelButton.addActionListener {
            this.dispose()
        }

        I18n.translateGui(this)
        isVisible = true
    }
}