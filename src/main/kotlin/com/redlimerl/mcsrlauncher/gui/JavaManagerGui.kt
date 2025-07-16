package com.redlimerl.mcsrlauncher.gui

import com.github.zafarkhaja.semver.Version
import com.redlimerl.mcsrlauncher.data.meta.MetaUniqueID
import com.redlimerl.mcsrlauncher.data.meta.file.JavaMetaFile
import com.redlimerl.mcsrlauncher.instance.JavaContainer
import com.redlimerl.mcsrlauncher.launcher.MetaManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.JavaUtils
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.*
import javax.swing.table.DefaultTableModel

class JavaManagerGui(parent: JDialog) : JavaManagerDialog(parent) {

    private val javaList = arrayListOf<JavaContainer>()

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
                for (javaMeta in MetaUniqueID.JAVA_METAS) {
                    MetaManager.getVersions(javaMeta, this).forEach { it.getOrLoadMetaVersionFile<JavaMetaFile>(javaMeta, this) }
                }
                updateDownloadJavaList()
                isVisible = true
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
    }

    fun updateInstalledJavaVersions() {
        javaList.clear()
        javaList.addAll(JavaUtils.javaHomeVersions())

        val tableModel =
            DefaultTableModel(arrayOf(), arrayOf(I18n.translate("text.version"), I18n.translate("text.path")))
        javaList.sortedByDescending { Version.parse(it.version.split("_").first()) }.forEach { tableModel.addRow(it.dataArray()) }

        javaListTable.model = tableModel
        if (javaListTable.rowCount > 0) javaListTable.setRowSelectionInterval(0, 0)
        selectJavaVersionButton.isEnabled = javaList.isNotEmpty()

        javaListTable.autoResizeMode = JTable.AUTO_RESIZE_OFF

        val columnModel = javaListTable.columnModel
        val fixedColWidth = 70
        columnModel.getColumn(0).preferredWidth = fixedColWidth

        if (javaListTable.parent != null && javaListTable.parent !is ComponentListener) {
            javaListTable.parent.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    val parent = javaListTable.parent ?: return
                    val viewportWidth = parent.width
                    val secondColWidth = (viewportWidth - fixedColWidth).coerceAtLeast(100) // 최소 100px 보장
                    if (columnModel.columnCount > 1) {
                        columnModel.getColumn(1).preferredWidth = secondColWidth
                    }
                    javaListTable.revalidate()
                    javaListTable.repaint()
                }
            })
        }
    }

    private fun initDownloadJavaTab() {
        javaVendorComboBox.addActionListener {
            updateDownloadJavaVersionList()
        }
        recommendedCheckBox.addActionListener {
            updateDownloadJavaVersionList()
        }
        javaVersionComboBox.addActionListener {
            if (javaVersionComboBox.selectedItem != null) updateDownloadJavaBuildList()
        }
        installJavaButton.isEnabled = false
        javaBuildComboBox.addActionListener {
            installJavaButton.isEnabled = javaBuildComboBox.selectedItem != null
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
    }

    private fun updateDownloadJavaBuildList(metaName: String = javaVendorComboBox.selectedItem!!.toString(), majorVersion: Int = javaVersionComboBox.selectedItem as Int) {
        val metaUniqueID = MetaUniqueID.JAVA_METAS.find { MetaManager.getMetaName(it) == metaName }!!
        val meta = MetaManager.getVersionMeta<JavaMetaFile>(metaUniqueID, "java$majorVersion") ?: throw IllegalStateException("$metaName JDK $majorVersion is not exist in meta")

        javaBuildComboBox.removeAllItems()
        for (runtime in meta.runtimes) {
            if (runtime.runtimeOS.isOn()) javaBuildComboBox.addItem(runtime.version.getName())
        }
    }
}