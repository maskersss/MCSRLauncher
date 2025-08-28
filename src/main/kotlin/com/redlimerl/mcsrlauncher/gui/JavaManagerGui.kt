package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.JavaMetaFile
import com.redlimerl.mcsrlauncher.instance.JavaContainer
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.JavaUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import com.redlimerl.mcsrlauncher.util.SwingUtils
import io.github.z4kn4fein.semver.toVersionOrNull
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.nio.file.Paths
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.table.DefaultTableModel

class JavaManagerGui(parent: JDialog, private val currentJavaPath: String, onSelect: (String) -> Unit) : JavaManagerDialog(parent) {

    private val javaList = arrayListOf<JavaContainer>()
    var selectedJavaPath: String? = null
        private set

    init {
        title = I18n.translate("text.settings.java")
        minimumSize = Dimension(700, 500)
        setLocationRelativeTo(parent)

        initInstalledJavaVersionsTab()
        initDownloadJavaTab()
        this.buttonCancel.addActionListener { this.dispose() }

        object :
            LauncherWorker(parent, I18n.translate("message.loading"), I18n.translate("message.loading.java_versions")) {
            override fun work(dialog: JDialog) {
                updateInstalledJavaVersions()
                updateDownloadJavaList()
                dialog.dispose()
                isVisible = true

                val result = selectedJavaPath
                if (result != null) onSelect(result)
            }
        }.showDialog().start()

        I18n.translateGui(this)
    }

    private fun initInstalledJavaVersionsTab() {
        javaListTable.tableHeader.reorderingAllowed = false
        javaListTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        javaListTable.setDefaultEditor(Object::class.java, null)
        javaListTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting && javaListTable.selectedRow == -1 && javaListTable.rowCount > 0)
                javaListTable.setRowSelectionInterval(it.lastIndex, it.lastIndex)
        }

        javaBrowseButton.addActionListener {
            val fileChooser = JFileChooser().apply {
                dialogType = JFileChooser.CUSTOM_DIALOG
                dialogTitle = I18n.translate("text.java.browse")
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = object : FileFilter() {
                    override fun accept(f: File): Boolean {
                        return f.isDirectory || f.name.equals(JavaUtils.javaExecutableName())
                    }

                    override fun getDescription(): String {
                        return JavaUtils.javaExecutableName()
                    }
                }
            }
            SwingUtils.makeEditablePathFileChooser(fileChooser)

            val result = fileChooser.showDialog(this, I18n.translate("text.select"))

            if (result == JFileChooser.APPROVE_OPTION) {
                MCSRLauncher.options.customJavaPaths.add(fileChooser.selectedFile.parentFile.parentFile.absolutePath)
                MCSRLauncher.options.save()
                updateInstalledJavaVersions()
                repeat(javaListTable.rowCount) { i ->
                    if (Paths.get(javaListTable.getValueAt(i, 2).toString()).parent.equals(fileChooser.selectedFile.parentFile.toPath())) {
                        javaListTable.setRowSelectionInterval(i, i)
                        return@repeat
                    }
                }
            }
        }

        selectJavaVersionButton.addActionListener {
            if (javaListTable.selectedRow >= 0) {
                selectedJavaPath = javaListTable.getValueAt(javaListTable.selectedRow, 2).toString()
            }
            this.dispose()
        }

        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                SwingUtilities.invokeLater {
                    SwingUtils.autoFitTableColumns(javaListTable, mapOf(1 to 130))
                }
            }
        })
    }

    fun updateInstalledJavaVersions() {
        javaList.clear()
        javaList.addAll(JavaUtils.javaHomeVersions())

        val tableModel =
            DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.version"), I18n.translate("text.vendor"), I18n.translate("text.path")))
        javaList.sortedByDescending {
            it.version.split("_").first().toVersionOrNull(false)
        }.forEach { tableModel.addRow(it.dataArray()) }

        javaListTable.model = tableModel
        if (javaListTable.rowCount > 0) javaListTable.setRowSelectionInterval(0, 0)
        selectJavaVersionButton.isEnabled = javaList.isNotEmpty()

        javaListTable.autoResizeMode = JTable.AUTO_RESIZE_OFF

        SwingUtilities.invokeLater {
            SwingUtils.autoFitTableColumns(javaListTable, mapOf(1 to 130))
        }

        repeat(javaListTable.rowCount) { i ->
            if (Paths.get(currentJavaPath).equals(Paths.get(javaListTable.getValueAt(i, 2).toString()))) {
                javaListTable.setRowSelectionInterval(i, i)
                return@repeat
            }
        }
    }

    private fun initDownloadJavaTab() {
        javaVendorComboBox.addActionListener {
            object : LauncherWorker(this@JavaManagerGui) {
                override fun work(dialog: JDialog) {
                    SwingUtils.setEnabledRecursively(javaTabPane.getComponentAt(1), false, javaLoadingLabel)
                    javaLoadingLabel.isVisible = true
                    val metaUniqueID = MetaUniqueID.JAVA_METAS.find { MetaManager.getMetaName(it) == javaVendorComboBox.selectedItem!!.toString() }!!
                    MetaManager.getVersions(metaUniqueID, this).forEach { it.getOrLoadMetaVersionFile<JavaMetaFile>(metaUniqueID, this) }
                    SwingUtilities.invokeLater {
                        SwingUtils.setEnabledRecursively(javaTabPane.getComponentAt(1), true)
                        updateDownloadJavaVersionList()
                        javaLoadingLabel.isVisible = false
                    }
                }
            }.start()
        }
        recommendedCheckBox.addActionListener {
            updateDownloadJavaVersionList()
        }
        javaVersionComboBox.addActionListener {
            if (javaVersionComboBox.selectedItem != null) updateDownloadJavaBuildList()
        }

        javaVersionComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val original = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (original is JLabel && value is Int) {
                    original.text = "Java - $value"
                }
                return original
            }
        }

        installJavaButton.isEnabled = false
        installJavaButton.addActionListener {
            val metaUniqueID = MetaUniqueID.JAVA_METAS.find { MetaManager.getMetaName(it) == javaVendorComboBox.selectedItem!!.toString() }!!
            val majorVersion = javaVersionComboBox.selectedItem as Int
            val meta = MetaManager.getVersionMeta<JavaMetaFile>(metaUniqueID, "java$majorVersion") ?: throw IllegalStateException("${javaVendorComboBox.selectedItem} JDK $majorVersion does not exist in meta")
            val buildVersion = javaBuildComboBox.selectedItem!!.toString()

            object : LauncherWorker(this@JavaManagerGui, I18n.translate("text.java.download"), I18n.translate("message.loading") + "...") {
                override fun work(dialog: JDialog) {
                    this.properties["download-java-version"] = buildVersion
                    meta.install(this)
                    updateInstalledJavaVersions()
                    dialog.dispose()
                    JOptionPane.showMessageDialog(this@JavaManagerGui, I18n.translate("message.install_java.success"))
                    javaTabPane.selectedIndex = 0
                }
            }.showDialog().start()
        }
    }

    private fun updateDownloadJavaList() {
        javaVendorComboBox.removeAllItems()
        for (javaMeta in MetaUniqueID.JAVA_METAS) {
            javaVendorComboBox.addItem(MetaManager.getMetaName(javaMeta))
        }
        updateDownloadJavaVersionList()
    }

    private fun updateDownloadJavaVersionList(metaName: String = javaVendorComboBox.selectedItem!!.toString()) {
        val beforeSelected = javaVersionComboBox.selectedItem
        javaVersionComboBox.removeAllItems()

        val metaUniqueID = MetaUniqueID.JAVA_METAS.find { MetaManager.getMetaName(it) == metaName }!!
        for (version in MetaManager.getVersions(metaUniqueID).sortedByDescending { it -> it.version.filter { it.isDigit() }.toInt() }) {
            if (version.recommended || !recommendedCheckBox.isSelected) {
                javaVersionComboBox.addItem(version.version.filter { it.isDigit() }.toInt())
            }
        }

        if (beforeSelected != null) {
            repeat(javaVersionComboBox.itemCount) {
                if (javaVersionComboBox.getItemAt(it) == beforeSelected) {
                    javaVersionComboBox.selectedIndex = it
                    return@repeat
                }
            }
        }

        installJavaButton.isEnabled = javaBuildComboBox.selectedItem != null
    }

    private fun updateDownloadJavaBuildList(metaName: String = javaVendorComboBox.selectedItem!!.toString(), majorVersion: Int = javaVersionComboBox.selectedItem as Int) {
        val metaUniqueID = MetaUniqueID.JAVA_METAS.find { MetaManager.getMetaName(it) == metaName }!!
        val meta = MetaManager.getVersionMeta<JavaMetaFile>(metaUniqueID, "java$majorVersion") ?: throw IllegalStateException("$metaName JDK $majorVersion does not exist in meta")

        javaBuildComboBox.removeAllItems()
        for (runtime in meta.runtimes) {
            if (runtime.runtimeOS.isOn())
                javaBuildComboBox.addItem(runtime.version.getName())
        }
    }
}