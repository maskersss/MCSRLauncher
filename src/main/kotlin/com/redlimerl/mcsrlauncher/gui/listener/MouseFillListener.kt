package com.redlimerl.mcsrlauncher.gui.listener

import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton

data class MouseFillListener(val button: JButton) : MouseListener {

    override fun mouseClicked(e: MouseEvent?) {
    }

    override fun mousePressed(e: MouseEvent?) {
    }

    override fun mouseReleased(e: MouseEvent?) {
    }

    override fun mouseEntered(e: MouseEvent?) {
        button.isContentAreaFilled = button.isFocusable
    }

    override fun mouseExited(e: MouseEvent?) {
        button.isContentAreaFilled = false
    }
}