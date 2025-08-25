package com.redlimerl.mcsrlauncher.gui.component

import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

object InstanceGroupComboBox {

    fun init(comboBox: JComboBox<String>) {
        val allItems = InstanceManager.instances.keys.toMutableList()
        if (!allItems.contains(InstanceManager.DEFAULT_GROUP)) allItems.add(InstanceManager.DEFAULT_GROUP)

        comboBox.isEditable = true
        comboBox.setModel(DefaultComboBoxModel(allItems.toTypedArray()))

//        val editor = comboBox.editor.editorComponent as JTextComponent
//
//        var lastFiltered: List<String> = allItems
//
//        editor.document.addDocumentListener(object : DocumentListener {
//            override fun insertUpdate(e: DocumentEvent) = update()
//            override fun removeUpdate(e: DocumentEvent) = update()
//            override fun changedUpdate(e: DocumentEvent) = update()
//
//            fun update() {
//                val input = editor.text
//                val filtered = allItems.filter { it.startsWith(input, ignoreCase = true) }
//
//                if (filtered == lastFiltered) return
//                lastFiltered = filtered
//
//                SwingUtilities.invokeLater {
//                    if (filtered.isEmpty()) {
//                        comboBox.hidePopup()
//                        return@invokeLater
//                    }
//
//                    comboBox.setModel(DefaultComboBoxModel(filtered.toTypedArray()))
//                    editor.text = input
//                    editor.caretPosition = input.length
//                    if (comboBox.isShowing) {
//                        comboBox.showPopup()
//                    }
//                }
//            }
//        })
    }
}