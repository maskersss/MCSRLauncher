package com.redlimerl.mcsrlauncher.gui.component

import com.formdev.flatlaf.FlatClientProperties
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.data.instance.FabricVersionData
import com.redlimerl.mcsrlauncher.data.instance.LWJGLVersionData
import com.redlimerl.mcsrlauncher.data.meta.IntermediaryType
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.MetaVersion
import com.redlimerl.mcsrlauncher.data.meta.MetaVersionType
import com.redlimerl.mcsrlauncher.gui.components.AbstractGameVersionsPanel
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SpeedrunUtils
import com.redlimerl.mcsrlauncher.util.SwingUtils
import io.github.z4kn4fein.semver.toVersion
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class GameVersionsPanel(private val parentWindow: JDialog, val instance: BasicInstance? = null) : AbstractGameVersionsPanel() {

    init {
        layout = BorderLayout()
        add(this.gameTabPane, BorderLayout.CENTER)

        initVanillaComponents()
        updateVanillaVersions()

        initFabricComponents()
        updateFabricVersions()

        if (instance != null) {
            val fabric = instance.fabricVersion
            if (fabric != null) {
                gameTabPane.selectedIndex = 0
                for (i in 0 until fabricVersionTable.rowCount) {
                    if (fabricVersionTable.getValueAt(i, 1) == instance.minecraftVersion) {
                        fabricVersionTable.setRowSelectionInterval(i, i)
                        fabricVersionTable.scrollRectToVisible(fabricVersionTable.getCellRect(i, 0, true))
                        break
                    }
                }
                intermediaryComboBox.selectedItem = fabric.intermediaryType
            } else {
                gameTabPane.selectedIndex = 1
            }

            for (i in 0 until vanillaVersionTable.rowCount) {
                if (vanillaVersionTable.getValueAt(i, 1) == instance.minecraftVersion) {
                    vanillaVersionTable.setRowSelectionInterval(i, i)
                    vanillaVersionTable.scrollRectToVisible(vanillaVersionTable.getCellRect(i, 0, true))
                    break
                }
            }
        }
    }

    fun getMinecraftVersion(): MetaVersion {
        val isFabric = gameTabPane.selectedIndex == 0
        val currentVersionTable = (if (isFabric) fabricVersionTable else vanillaVersionTable)
        if (currentVersionTable.selectedRow == -1) throw IllegalStateException("Minecraft version has not selected")

        val selectedMinecraftVersion = currentVersionTable.let { it.getValueAt(it.selectedRow, 1) }.toString()
        return MetaManager.getVersions(MetaUniqueID.MINECRAFT).find { it.version == selectedMinecraftVersion }!!
    }

    fun getLWJGLVersion(): LWJGLVersionData {
        val vanillaVersion = this.getMinecraftVersion()
        val isFabric = gameTabPane.selectedIndex == 0
        val currentLWJGLComboBox = (if (isFabric) fabricLWJGLComboBox else vanillaLWJGLComboBox)
        val lwjglRequire = vanillaVersion.requires.first()
        val lwjglSelected = currentLWJGLComboBox.selectedItem as? String ?: throw IllegalStateException("LWJGL version is not selected")
        if (MetaManager.getVersions(lwjglRequire.uid).none { it.version == lwjglSelected }) throw IllegalStateException("$lwjglSelected is not selectable version in ${lwjglRequire.uid}")
        return LWJGLVersionData(lwjglRequire.uid, currentLWJGLComboBox.selectedItem as? String ?: throw IllegalStateException("LWJGL version is not selected"))
    }

    fun getFabricVersion(): FabricVersionData? {
        val isFabric = gameTabPane.selectedIndex == 0
        val fabricVersion = (if (isFabric) fabricLoaderVersionComboBox.model.selectedItem?.toString() else null)
            ?.let { MetaManager.getVersions(MetaUniqueID.FABRIC_LOADER).find { loader -> loader.version == it } }
        val vanillaVersion = this.getMinecraftVersion()
        val intermediaryType = this.getIntermediaryType()

        return if (fabricVersion != null && intermediaryType != null) {
            val loaderVersion = fabricVersion.version
            val intermediaryVersion = vanillaVersion.version
            FabricVersionData(loaderVersion, intermediaryType, intermediaryVersion)
        } else null
    }

    private fun getIntermediaryType(): IntermediaryType? {
        return intermediaryComboBox.selectedItem as IntermediaryType?
    }

    private fun initVanillaComponents() {
        vanillaRefreshButton.addActionListener {
            object : LauncherWorker(parent, I18n.translate("message.loading"), I18n.translate("message.updating.meta")) {
                override fun work(dialog: JDialog) {
                    MetaManager.load(this, true)
                    updateVanillaVersions()
                }
            }.showDialog().start()
        }

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
        vanillaVersionTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val selectedRow = vanillaVersionTable.selectedRow
                if (selectedRow >= 0) {
                    this.onSelectVanillaMinecraftVersion(vanillaVersionTable.getValueAt(selectedRow, 1).toString())
                }
            }
        }

        val tableModel = DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.type"), I18n.translate("text.version"), I18n.translate("text.date")))
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
        SwingUtils.autoFitTableColumns(vanillaVersionTable)
    }

    private fun initFabricComponents() {
        fabricRefreshButton.addActionListener {
            object : LauncherWorker(parentWindow, I18n.translate("message.loading"), I18n.translate("message.updating.versions")) {
                override fun work(dialog: JDialog) {
                    MetaManager.load(this, true)
                    updateVanillaVersions()
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

        intermediaryHelpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP)
        intermediaryHelpButton.addActionListener {
            JOptionPane.showMessageDialog(parent, I18n.translate("message.help.intermediary"))
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
        val tableModel = DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.type"), I18n.translate("text.version"), I18n.translate("text.date")))
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
        SwingUtils.autoFitTableColumns(fabricVersionTable)
    }

    private fun onSelectFabricMinecraftVersion(version: String) {
        intermediaryComboBox.removeAllItems()
        val intermediaryVersion = MetaManager.getVersions(MetaUniqueID.FABRIC_INTERMEDIARY).find { it.version == version }!!
        for (compatibleIntermediary in intermediaryVersion.compatibleIntermediaries.sortedByDescending { it.recommendLevel }) {
            intermediaryComboBox.addItem(compatibleIntermediary)
        }

        val minecraftVersion = MetaManager.getVersions(MetaUniqueID.MINECRAFT).find { it.version == version }!!

        fabricLWJGLComboBox.removeAllItems()
        val lwjglRequire = minecraftVersion.requires.first()
        val availableLWJGL = MetaManager.getVersions(lwjglRequire.uid)
            .filter { it.version.toVersion() >= lwjglRequire.suggests?.toVersion()!! }
            .sortedByDescending { it.version.toVersion() }
        availableLWJGL.forEach { fabricLWJGLComboBox.addItem(it.version) }
        fabricLWJGLComboBox.selectedIndex = availableLWJGL.indexOfFirst {
            it.version == (instance?.lwjglVersion?.version ?: lwjglRequire.suggests)
        }
    }

    private fun onSelectVanillaMinecraftVersion(version: String) {
        val minecraftVersion = MetaManager.getVersions(MetaUniqueID.MINECRAFT).find { it.version == version }!!

        vanillaLWJGLComboBox.removeAllItems()
        val lwjglRequire = minecraftVersion.requires.first()
        val availableLWJGL = MetaManager.getVersions(lwjglRequire.uid)
            .filter { it.version.toVersion() >= lwjglRequire.suggests?.toVersion()!! }
            .sortedByDescending { it.version.toVersion() }
        availableLWJGL.forEach { vanillaLWJGLComboBox.addItem(it.version) }
        vanillaLWJGLComboBox.selectedIndex = availableLWJGL.indexOfFirst {
            it.version == (instance?.lwjglVersion?.version ?: lwjglRequire.suggests)
        }
    }

}