package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.gui.component.InstanceGroupComboBox
import com.redlimerl.mcsrlauncher.gui.component.JavaSettingsPanel
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.SwingUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Window
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class InstanceOptionGui(parent: Window, val instance: BasicInstance) : InstanceOptionDialog(parent) {

    init {
        title = I18n.translate("text.settings")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        this.cancelButton.addActionListener { this.dispose() }

        initInstanceTab()
        initVersionTab()
        initJavaTab()

        I18n.translateGui(this)
        isVisible = true
    }

    private fun initInstanceTab() {
        instanceNameField.text = instance.displayName

        InstanceGroupComboBox.init(instanceGroupField)
        instanceGroupField.selectedItem = InstanceManager.getInstanceGroup(instance)

        instanceApplyChangesButton.addActionListener {
            if (instanceNameField.text != instance.displayName) {
                InstanceManager.renameInstance(instance, instanceNameField.text)
            }
            if (instanceGroupField.selectedItem?.toString() != InstanceManager.getInstanceGroup(instance)) {
                InstanceManager.moveInstanceGroup(instance, instanceGroupField.selectedItem as String)
            }
        }
    }

    private fun initVersionTab() {
        versionsTable.tableHeader.reorderingAllowed = false
        versionsTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        versionsTable.setDefaultEditor(Object::class.java, null)
        versionsTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting && versionsTable.selectedRow == -1 && versionsTable.rowCount > 0)
                versionsTable.setRowSelectionInterval(it.lastIndex, it.lastIndex)
        }

        val tableModel = DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.type"), I18n.translate("text.version")))
        tableModel.addRow(arrayOf("Minecraft", instance.minecraftVersion))
        tableModel.addRow(arrayOf("LWJGL", instance.lwjglVersion.version))
        val fabric = instance.fabricVersion
        if (fabric != null) {
            tableModel.addRow(arrayOf("Fabric Loader", fabric.loaderVersion))
            tableModel.addRow(arrayOf("Fabric Intermediary", "${fabric.intermediaryVersion} (${fabric.intermediaryType.intermediaryName})"))
        }
        versionsTable.model = tableModel

        if (changeVersionButton.actionListeners.isEmpty()) {
            changeVersionButton.addActionListener {
                val changeVersion = ChangeGameVersionGui(this@InstanceOptionGui, instance)
                if (changeVersion.hasChanged) {
                    initVersionTab()
                }
            }
        }
    }

    private fun initJavaTab() {
        val javaSettingsPanel = JavaSettingsPanel(this, instance.options, instance.options::save)

        javaLauncherSettingCheckBox.addActionListener {
            javaSettingsPanel.setAllEnabled(!javaLauncherSettingCheckBox.isSelected)
            instance.options.useLauncherOption = javaLauncherSettingCheckBox.isSelected
            instance.options.save()
        }
        javaLauncherSettingCheckBox.isSelected = instance.options.useLauncherOption
        javaSettingsPanel.setAllEnabled(!javaLauncherSettingCheckBox.isSelected)

        javaSettingsPane.layout = BorderLayout()
        javaSettingsPane.add(javaSettingsPanel, BorderLayout.CENTER)
        SwingUtils.fasterScroll(javaScrollPane)
    }
}