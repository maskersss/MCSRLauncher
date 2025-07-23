package com.redlimerl.mcsrlauncher.util

import java.awt.Container
import java.io.File
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxEditor

object SwingUtils {

    fun fasterScroll(scrollPane: JScrollPane) {
        scrollPane.horizontalScrollBar.unitIncrement *= 15
        scrollPane.verticalScrollBar.unitIncrement *= 15
    }

    fun autoResizeColumnWidths(table: JTable, preferredMaxWidths: Map<Int, Int> = emptyMap()) {
        val model = table.model
        val colCount = table.columnCount

        val widths = IntArray(colCount)

        for (col in 0 until colCount) {
            var maxWidth = 0

            val headerRenderer = table.tableHeader.defaultRenderer
            val headerValue = table.columnModel.getColumn(col).headerValue
            val headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, -1, col)
            maxWidth = maxOf(maxWidth, headerComp.preferredSize.width)

            for (row in 0 until table.rowCount) {
                val value = model.getValueAt(row, col)
                val renderer = table.getCellRenderer(row, col)
                val comp = renderer.getTableCellRendererComponent(table, value, false, false, row, col)
                maxWidth = maxOf(maxWidth, comp.preferredSize.width)
            }

            val preferredMax = preferredMaxWidths[col]
            val finalWidth = if (preferredMax != null) minOf(maxWidth, preferredMax) else maxWidth

            widths[col] = finalWidth + 12
        }

        val viewportWidth = (table.parent as? JViewport)?.width ?: table.width
        val sumExceptLast = widths.dropLast(1).sum()
        val lastColOriginalWidth = widths.last()
        val lastColAdjustedWidth =
            if (sumExceptLast + lastColOriginalWidth <= viewportWidth) lastColOriginalWidth
            else (viewportWidth - sumExceptLast).coerceAtLeast(100)

        for (col in 0 until colCount - 1) {
            table.columnModel.getColumn(col).preferredWidth = widths[col]
        }

        table.columnModel.getColumn(colCount - 1).preferredWidth = lastColAdjustedWidth
    }

    fun makeEditablePathFileChooser(fileChooser: JFileChooser) {
        val jComboBox = getDescendantsOfType(JComboBox::class.java, fileChooser).first()
        jComboBox.isEditable = true
        jComboBox.editor = object : BasicComboBoxEditor.UIResource() {
            override fun getItem(): Any {
                return try {
                    File(super.getItem() as String)
                } catch (e: Exception) {
                    super.getItem()
                }
            }
        }
    }

    fun <T : JComponent?> getDescendantsOfType(clazz: Class<T>, container: Container): List<T> {
        return getDescendantsOfType(clazz, container, true)
    }

    fun <T : JComponent?> getDescendantsOfType(clazz: Class<T>, container: Container, nested: Boolean): List<T> {
        val tList: MutableList<T> = ArrayList()
        for (component in container.components) {
            if (clazz.isAssignableFrom(component.javaClass)) {
                tList.add(clazz.cast(component))
            }
            if (nested || !clazz.isAssignableFrom(component.javaClass)) {
                tList.addAll(getDescendantsOfType(clazz, component as Container, nested))
            }
        }
        return tList
    }

    private fun measureHtmlTextWidth(html: String): Int {
        val label = JLabel(html)
        val fakeParent = JPanel() // Layout 연산을 강제하기 위한 가짜 컨테이너
        fakeParent.add(label)
        label.doLayout()
        label.validate()
        return label.preferredSize.width
    }

    fun autoResizeHtmlText(
        rawText: String,
        maxWidth: Int,
        baseFontSize: Int = 16
    ): String {
        var fontSize = baseFontSize
        var html: String
        var width: Int

        do {
            html = "<span style='font-size:${fontSize}px;'>$rawText</span>"
            width = measureHtmlTextWidth("<html>$html</html>")
            if (width <= maxWidth) break
            fontSize--
        } while (fontSize > 4)

        return html
    }

}