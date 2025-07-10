package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.data.meta.IntermediaryType
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.MetaVersionType
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SpeedrunUtils
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel


class CreateInstanceGui(parent: JFrame) : CreateInstanceDialog(parent) {

    init {
        title = I18n.translate("instance.new")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        instanceNameField.text = I18n.translate("instance.new")

        cancelButton.addActionListener { this.dispose() }
        createInstanceButton.addActionListener { this.createInstance() }

        initVanillaComponents()
        updateVanillaVersions()

        initFabricComponents()
        updateFabricVersions()

        I18n.translateGui(this)
        isVisible = true
    }

    private fun initVanillaComponents() {
        vanillaRefreshButton.addActionListener { refreshMeta() }

        listOf(vanillaSpeedrunCheckbox, vanillaReleaseCheckBox, vanillaSnapshotCheckBox, vanillaBetaCheckBox, vanillaAlphaCheckBox, vanillaExperimentCheckBox)
            .forEach { it.addActionListener { updateVanillaVersions() } }

        vanillaVersionSearchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateVanillaVersions()
            override fun removeUpdate(e: DocumentEvent?) = updateVanillaVersions()
            override fun changedUpdate(e: DocumentEvent?) = Unit
        })
        vanillaVersionTable.tableHeader.reorderingAllowed = false
        vanillaVersionTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        vanillaVersionTable.setDefaultEditor(Object::class.java, null)
        vanillaVersionTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting && vanillaVersionTable.selectedRow == -1 && vanillaVersionTable.rowCount > 0)
                vanillaVersionTable.setRowSelectionInterval(it.lastIndex, it.lastIndex)
        }
    }

    private fun updateVanillaVersions() {
        val tableModel = DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.type.title"), I18n.translate("text.version.title"), I18n.translate("text.date")))
        MetaManager.getVersions(MetaUniqueID.MINECRAFT).filter {
            if (it.type == MetaVersionType.RELEASE && !vanillaReleaseCheckBox.isSelected
                && (!vanillaSpeedrunCheckbox.isSelected || !SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.contains(it.version))) return@filter false
            if ((it.type == MetaVersionType.SNAPSHOT || it.type == MetaVersionType.OLD_SNAPSHOT) && !vanillaSnapshotCheckBox.isSelected) return@filter false
            if ((it.type == MetaVersionType.BETA || it.type == MetaVersionType.OLD_BETA) && !vanillaBetaCheckBox.isSelected) return@filter false
            if ((it.type == MetaVersionType.ALPHA || it.type == MetaVersionType.OLD_ALPHA) && !vanillaAlphaCheckBox.isSelected) return@filter false
            if (it.type == MetaVersionType.EXPERIMENT && !vanillaExperimentCheckBox.isSelected) return@filter false
            if (vanillaVersionSearchField.text.isNotBlank() && !it.version.contains(vanillaVersionSearchField.text, true)) return@filter false
            return@filter true
        }.sortedByDescending {
            if (vanillaSpeedrunCheckbox.isSelected && SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.contains(it.version)) return@sortedByDescending Long.MAX_VALUE - SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.indexOf(it.version)
            return@sortedByDescending it.releaseTime.time
        }.forEach {
            tableModel.addRow(it.dataArray())
        }

        vanillaVersionTable.model = tableModel
        if (vanillaVersionTable.rowCount > 0) vanillaVersionTable.setRowSelectionInterval(0, 0)
    }

    private fun initFabricComponents() {
        fabricRefreshButton.addActionListener {
            object : LauncherWorker(this@CreateInstanceGui, I18n.translate("message.loading"), I18n.translate("message.updating.versions")) {
                override fun work(dialog: JDialog) {
                    MetaManager.load(this, true)
                    updateFabricVersions()
                }
            }.showDialog().start()
        }

        listOf(fabricSpeedrunCheckBox, fabricReleaseCheckBox, fabricSnapshotCheckBox, fabricBetaCheckBox, fabricAlphaCheckBox, fabricExperimentCheckBox).forEach { it.addActionListener { updateFabricVersions() } }

        fabricVersionSearchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateFabricVersions()
            override fun removeUpdate(e: DocumentEvent?) = updateFabricVersions()
            override fun changedUpdate(e: DocumentEvent?) = Unit
        })
        fabricVersionTable.tableHeader.reorderingAllowed = false
        fabricVersionTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        fabricVersionTable.setDefaultEditor(Object::class.java, null)
        fabricVersionTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting && fabricVersionTable.selectedRow == -1 && fabricVersionTable.rowCount > 0)
                fabricVersionTable.setRowSelectionInterval(it.lastIndex, it.lastIndex)
        }

        intermediaryHelpButton.addActionListener {
            JOptionPane.showMessageDialog(this@CreateInstanceGui, I18n.translate("message.help.intermediary"))
        }
    }

    private fun updateFabricVersions() {
        val loaderModel = DefaultComboBoxModel<String>(arrayOf())
        val fabricLoaderVersions = MetaManager.getVersions(MetaUniqueID.FABRIC_LOADER)

        fabricLoaderVersions.forEach { loaderModel.addElement(it.version) }
        fabricLoaderVersionComboBox.model = loaderModel
        fabricLoaderVersionComboBox.selectedIndex = fabricLoaderVersions.indexOfFirst { it.recommended }

        fabricVersionTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val selectedRow = fabricVersionTable.selectedRow
                if (selectedRow >= 0) {
                    this.onSelectFabricMinecraftVersion(fabricVersionTable.getValueAt(selectedRow, 1).toString())
                }
            }
        }
        val tableModel = DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.type.title"), I18n.translate("text.version.title"), I18n.translate("text.date")))
        MetaManager.getVersions(MetaUniqueID.MINECRAFT).filter {
            if (!MetaManager.containsVersion(MetaUniqueID.FABRIC_INTERMEDIARY, it.version)) return@filter false
            if (it.type == MetaVersionType.RELEASE && !fabricReleaseCheckBox.isSelected
                && (!fabricSpeedrunCheckBox.isSelected || !SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.contains(it.version))) return@filter false
            if ((it.type == MetaVersionType.SNAPSHOT || it.type == MetaVersionType.OLD_SNAPSHOT) && !fabricSnapshotCheckBox.isSelected) return@filter false
            if ((it.type == MetaVersionType.BETA || it.type == MetaVersionType.OLD_BETA) && !fabricBetaCheckBox.isSelected) return@filter false
            if ((it.type == MetaVersionType.ALPHA || it.type == MetaVersionType.OLD_ALPHA) && !fabricAlphaCheckBox.isSelected) return@filter false
            if (it.type == MetaVersionType.EXPERIMENT && !fabricExperimentCheckBox.isSelected) return@filter false
            if (fabricVersionSearchField.text.isNotBlank() && !it.version.contains(fabricVersionSearchField.text, true)) return@filter false
            return@filter true
        }.sortedByDescending {
            if (fabricSpeedrunCheckBox.isSelected && SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.contains(it.version)) return@sortedByDescending Long.MAX_VALUE - SpeedrunUtils.MAJOR_SPEEDRUN_VERSIONS.indexOf(it.version)
            return@sortedByDescending it.releaseTime.time
        }.forEach {
            tableModel.addRow(it.dataArray())
        }
        fabricVersionTable.model = tableModel
        if (fabricVersionTable.rowCount > 0) fabricVersionTable.setRowSelectionInterval(0, 0)
    }

    private fun onSelectFabricMinecraftVersion(version: String) {
        intermediaryComboBox.removeAllItems()
        val intermediaryVersion = MetaManager.getVersions(MetaUniqueID.FABRIC_INTERMEDIARY).find { it.version == version }!!
        for (compatibleIntermediary in intermediaryVersion.compatibleIntermediaries.sortedByDescending { it.recommendLevel }) {
            intermediaryComboBox.addItem(compatibleIntermediary)
        }
    }

    private fun createInstance() {
        if (instanceNameField.text.isNullOrBlank()) return

        val isFabric = gameTabPane.selectedIndex == 0
        val currentVersionTable = (if (isFabric) fabricVersionTable else vanillaVersionTable)
        if (currentVersionTable.selectedRow == -1) return

        val selectedMinecraftVersion = currentVersionTable.let { it.getValueAt(it.selectedRow, 1) }.toString()
        val vanillaVersion = MetaManager.getVersions(MetaUniqueID.MINECRAFT).find { it.version == selectedMinecraftVersion }!!
        val fabricVersion = (if (isFabric) fabricLoaderVersionComboBox.model.selectedItem?.toString() else null)
            ?.let { MetaManager.getVersions(MetaUniqueID.FABRIC_LOADER).find { loader -> loader.version == it } }

        val instance = InstanceManager.createInstance(instanceNameField.text, vanillaVersion, fabricVersion, intermediaryComboBox.selectedItem as IntermediaryType?)

        InstanceManager.addInstance(instance, instanceGroupBox.selectedItem?.toString()?.trimEnd())
        this.dispose()
    }

    private fun refreshMeta() {
        object : LauncherWorker(this@CreateInstanceGui, I18n.translate("message.loading"), I18n.translate("message.updating.meta")) {
            override fun work(dialog: JDialog) {
                MetaManager.load(this, true)
                updateVanillaVersions()
                updateFabricVersions()
            }
        }.showDialog().start()
    }

}