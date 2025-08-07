package com.redlimerl.mcsrlauncher.gui.component

import com.redlimerl.mcsrlauncher.data.launcher.LauncherSharedOptions
import com.redlimerl.mcsrlauncher.gui.components.AbstractResolutionSettingsPanel
import java.awt.BorderLayout
import javax.swing.SpinnerNumberModel

class ResolutionSettingsPanel(val options: LauncherSharedOptions, private val onUpdate: () -> Unit) : AbstractResolutionSettingsPanel() {

    init {
        layout = BorderLayout()
        add(this.rootPanel, BorderLayout.CENTER)
        this.widthSpinner.model = SpinnerNumberModel(options.resolutionWidth, 100, 25565, 1)
        this.heightSpinner.model = SpinnerNumberModel(options.resolutionHeight, 100, 25565, 1)
        this.widthSpinner.addChangeListener {
            options.resolutionWidth = this.widthSpinner.value as Int
            onUpdate()
        }
        this.heightSpinner.addChangeListener {
            options.resolutionHeight = this.heightSpinner.value as Int
            onUpdate()
        }
    }

}