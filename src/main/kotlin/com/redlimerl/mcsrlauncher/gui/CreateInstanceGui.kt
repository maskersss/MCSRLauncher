package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.gui.component.GameVersionsPanel
import com.redlimerl.mcsrlauncher.gui.component.InstanceGroupComboBox
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.I18n
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JOptionPane


class CreateInstanceGui(parent: JFrame) : CreateInstanceDialog(parent) {

    private val gameVersionsPanel: GameVersionsPanel

    init {
        title = I18n.translate("instance.new")
        minimumSize = Dimension(700, 550)
        setLocationRelativeTo(parent)

        instanceNameField.text = I18n.translate("instance.new")
        InstanceGroupComboBox.init(instanceGroupBox)

        cancelButton.addActionListener { this.dispose() }
        createInstanceButton.addActionListener { this.createInstance() }

        this.gameVersionsPanel = GameVersionsPanel(this)
        versionsPanel.layout = BorderLayout()
        versionsPanel.add(this.gameVersionsPanel, BorderLayout.CENTER)

        I18n.translateGui(this)
        isVisible = true
    }

    private fun createInstance() {
        if (instanceNameField.text.isNullOrBlank()) return

        val instance = InstanceManager.createInstance(instanceNameField.text, gameVersionsPanel.getMinecraftVersion(), gameVersionsPanel.getLWJGLVersion(), gameVersionsPanel.getFabricVersion())

        InstanceManager.addInstance(instance, instanceGroupBox.selectedItem?.toString()?.trimEnd())

        this.dispose()
        val launch = {
            val launchConfirm = JOptionPane.showConfirmDialog(this, I18n.translate("message.download_success") + "\n" + I18n.translate("message.instance_launch_ask"), I18n.translate("instance.launch"), JOptionPane.YES_NO_OPTION)
            if (launchConfirm == JOptionPane.YES_OPTION) {
                instance.launchWithDialog()
            }
        }

        if (instance.fabricVersion != null) {
            val modInit = JOptionPane.showConfirmDialog(this, I18n.translate("message.speedrun_mods_setup_ask"), I18n.translate("text.manage_speedrun_mods"), JOptionPane.YES_NO_OPTION)
            if (modInit == JOptionPane.YES_OPTION) {
                ManageSpeedrunModsGui(this, instance, true) {
                    launch()
                }
                return
            }
        }
        launch()
    }

}