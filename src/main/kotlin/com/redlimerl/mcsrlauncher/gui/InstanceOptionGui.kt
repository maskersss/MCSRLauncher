package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.gui.component.InstanceGroupComboBox
import com.redlimerl.mcsrlauncher.gui.component.JavaSettingsPanel
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.SwingUtils
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Window
import java.text.SimpleDateFormat
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class InstanceOptionGui(parent: Window, val instance: BasicInstance) : InstanceOptionDialog(parent) {

    init {
        title = getUpdatedTitle()
        minimumSize = Dimension(800, 500)
        setLocationRelativeTo(parent)

        this.cancelButton.addActionListener { this.dispose() }

        initInstanceTab()
        initVersionTab()
        initModsTab()
        initJavaTab()

        I18n.translateGui(this)
        isVisible = true
    }

    private fun getUpdatedTitle(): String {
        return "${I18n.translate("instance.edit")} - ${instance.displayName}"
    }

    private fun initInstanceTab() {
        instanceNameField.text = instance.displayName

        InstanceGroupComboBox.init(instanceGroupField)
        instanceGroupField.selectedItem = InstanceManager.getInstanceGroup(instance)

        instanceApplyChangesButton.addActionListener {
            if (instanceNameField.text != instance.displayName) {
                InstanceManager.renameInstance(instance, instanceNameField.text)
                title = getUpdatedTitle()
            }
            if (instanceGroupField.selectedItem?.toString() != InstanceManager.getInstanceGroup(instance)) {
                InstanceManager.moveInstanceGroup(instance, instanceGroupField.selectedItem as String)
            }
        }

        instanceOpenDirectoryButton.addActionListener {
            Desktop.getDesktop().open(instance.getGamePath().toFile().apply { mkdirs() })
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

    private fun initModsTab() {
        manageSpeedrunModsButton.addActionListener {
            ManageSpeedrunModsGui(this)
        }

        modsTable.tableHeader.reorderingAllowed = false
        modsTable.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        modsTable.setDefaultEditor(Object::class.java, null)

        val tableModel = DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.name"), I18n.translate("text.version"), I18n.translate("text.last_modified"), I18n.translate("text.size")))
        instance.getMods().forEach {
            tableModel.addRow(arrayOf(it.name, it.version, SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, MCSRLauncher.options.language.getLocale()).format(it.file.lastModified()), AssetUtils.formatFileSize(it.file.length())))
        }
        modsTable.model = tableModel

        TODO("add file - mod enable/disable/remove")
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