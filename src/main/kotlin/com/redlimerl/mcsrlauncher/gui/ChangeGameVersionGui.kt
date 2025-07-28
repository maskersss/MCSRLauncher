package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.gui.component.GameVersionsPanel
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.I18n
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JDialog

class ChangeGameVersionGui(parent: JDialog, val instance: BasicInstance) : ChangeGameVersionDialog(parent) {

    private val gameVersionsPanel: GameVersionsPanel
    var hasChanged = false

    init {
        title = I18n.translate("text.version.change")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        this.gameVersionsPanel = GameVersionsPanel(instance)
        versionsPanel.layout = BorderLayout()
        versionsPanel.add(this.gameVersionsPanel, BorderLayout.CENTER)

        changeButton.addActionListener {
            instance.minecraftVersion = this.gameVersionsPanel.getMinecraftVersion().version
            instance.lwjglVersion = this.gameVersionsPanel.getLWJGLVersion()
            instance.fabricVersion = this.gameVersionsPanel.getFabricVersion()
            InstanceManager.refreshInstanceList()
            InstanceManager.save()
            hasChanged = true
            this.dispose()
        }

        cancelButton.addActionListener {
            this.dispose()
        }

        I18n.translateGui(this)
        isVisible = true
    }
}