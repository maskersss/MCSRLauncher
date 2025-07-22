package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.gui.component.GameVersionsPanel
import com.redlimerl.mcsrlauncher.gui.component.InstanceGroupComboBox
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.I18n
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame


class CreateInstanceGui(parent: JFrame) : CreateInstanceDialog(parent) {

    private val gameVersionsPanel: GameVersionsPanel

    init {
        title = I18n.translate("instance.new")
        minimumSize = Dimension(700, 500)
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
    }

}