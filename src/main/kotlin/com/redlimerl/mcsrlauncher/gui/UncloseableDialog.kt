@file:Suppress("LeakingThis")

package com.redlimerl.mcsrlauncher.gui

import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Frame
import javax.swing.*

class UncloseableDialog : JDialog {

    private val label = JLabel("", SwingConstants.CENTER)

    constructor(title: String, labelText: String) : super(null as Dialog?, title, true) {
        init(labelText)
        setLocationRelativeTo(null)
        javaClass.classLoader.getResource("icon.png")?.let { setIconImage(ImageIcon(it).image) }
    }

    constructor(owner: Frame?, title: String, labelText: String) : super(owner, title, true) {
        init(labelText)
        setLocationRelativeTo(owner)
    }

    constructor(owner: Dialog?, title: String, labelText: String) : super(owner, title, true) {
        init(labelText)
        setLocationRelativeTo(owner)
    }

    private fun init(labelText: String) {
        label.font = label.font.let { it.deriveFont(it.size + 2f) }
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        isResizable = false
        layout = BorderLayout(10, 10)
        setText(labelText)
        add(label, BorderLayout.CENTER)
    }

    fun setText(text: String) {
        label.text = text
        minimumSize = Dimension(minimumSize.width.coerceAtLeast((label.preferredSize.width + 60).coerceAtLeast(300)), label.preferredSize.height + 90)
    }

}