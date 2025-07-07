package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.util.I18n
import java.awt.Dimension
import javax.swing.JFrame

class LauncherOptionGui(parent: JFrame) : LauncherOptionDialog(parent) {

    init {
        title = I18n.translate("text.settings")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        I18n.translateGui(this)
        isVisible = true
    }
}