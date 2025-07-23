package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.gui.component.InstanceGroupComboBox
import com.redlimerl.mcsrlauncher.gui.component.JavaSettingsPanel
import com.redlimerl.mcsrlauncher.instance.mod.ModData
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.AssetUtils
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.SwingUtils
import org.apache.commons.io.FileUtils
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Window
import java.io.File
import java.text.SimpleDateFormat
import javax.swing.JFileChooser
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileFilter
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class InstanceOptionGui(parent: Window, val instance: BasicInstance) : InstanceOptionDialog(parent) {

    var mods: List<ModData> = emptyList()

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

    private fun updateMods() {
        this.mods = instance.getMods()

        modsTable.tableHeader.reorderingAllowed = false
        modsTable.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        modsTable.setDefaultEditor(Object::class.java, null)

        val modTableModel = object : AbstractTableModel() {
            override fun getRowCount(): Int = mods.size

            override fun getColumnCount(): Int = 5

            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
                val mod = mods[rowIndex]
                return when(columnIndex) {
                    0 -> mod.name
                    1 -> I18n.translate(if (mod.isEnabled) "text.enabled" else "text.disabled")
                    2 -> mod.version
                    3 -> SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT, MCSRLauncher.options.language.getLocale()).format(mod.file.lastModified())
                    4 -> AssetUtils.formatFileSize(mod.file.length())
                    else -> ""
                }
            }

            override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java

            override fun getColumnName(column: Int): String {
                return arrayOf(I18n.translate("text.name"), I18n.translate("text.enabled"), I18n.translate("text.version"), I18n.translate("text.last_modified"), I18n.translate("text.size"))[column]
            }
        }
        modsTable.model = modTableModel

        SwingUtilities.invokeLater {
            SwingUtils.autoResizeColumnWidths(modsTable)
        }
    }

    private fun initModsTab() {
        manageSpeedrunModsButton.addActionListener {
            ManageSpeedrunModsGui(this)
        }

        updateMods()

        fun updateModSelection() {
            val enabled = modsTable.selectedRows.isNotEmpty()
            enableModButton.isEnabled = enabled
            disableModButton.isEnabled = enabled
            deleteModButton.isEnabled = enabled
        }

        modsTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                updateModSelection()
            }
        }

        updateModSelection()

        addModFileButton.addActionListener {
            val fileChooser = JFileChooser(instance.getModsPath().toFile()).apply {
                isMultiSelectionEnabled = true
                dialogType = JFileChooser.CUSTOM_DIALOG
                dialogTitle = I18n.translate("text.add_mod_file")
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = object : FileFilter() {
                    override fun accept(f: File): Boolean {
                        return f.isDirectory || f.name.lowercase().endsWith(".jar")
                    }

                    override fun getDescription(): String {
                        return "JAR File (*.jar)"
                    }
                }
            }
            SwingUtils.makeEditablePathFileChooser(fileChooser)

            val result = fileChooser.showDialog(this, I18n.translate("text.select"))

            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFiles = fileChooser.selectedFiles
                for (file in selectedFiles) {
                    if (file.parentFile.absolutePath == instance.getModsPath().absolutePathString() || instance.getModsPath().resolve(file.name).exists()) continue
                    if (ModData.get(file) == null) continue
                    FileUtils.copyFile(file, instance.getModsPath().resolve(file.name).toFile())
                }
                updateMods()
            }
        }

        enableModButton.addActionListener {
            for (selectedRow in modsTable.selectedRows) {
                mods[selectedRow].isEnabled = true
                updateMods()
            }
        }

        disableModButton.addActionListener {
            for (selectedRow in modsTable.selectedRows) {
                mods[selectedRow].isEnabled = false
                updateMods()
            }
        }

        deleteModButton.addActionListener {
            for (selectedRow in modsTable.selectedRows) {
                mods[selectedRow].delete()
                updateMods()
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