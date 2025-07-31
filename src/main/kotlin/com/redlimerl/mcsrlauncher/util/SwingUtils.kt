package com.redlimerl.mcsrlauncher.util

import java.awt.Component
import java.awt.Container
import java.io.File
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxEditor

object SwingUtils {

    fun fasterScroll(scrollPane: JScrollPane) {
        scrollPane.horizontalScrollBar.unitIncrement *= 15
        scrollPane.verticalScrollBar.unitIncrement *= 15
    }

    fun autoFitTableColumns(table: JTable, preferredMaxWidths: Map<Int, Int> = emptyMap()) {
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

        fun <T : JComponent?> getDescendantsOfType(clazz: Class<T>, container: Container): List<T> {
            return getDescendantsOfType(clazz, container, true)
        }

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

    fun autoFitHtmlText(rawText: String, maxWidth: Int, baseFontSize: Int = 16, minimumFontSize: Int = 6): String {
        fun measureHtmlTextWidth(html: String): Int {
            val label = JLabel(html)
            val fakeParent = JPanel()
            fakeParent.add(label)
            label.doLayout()
            label.validate()
            return label.preferredSize.width
        }

        fun breakLongWord(word: String, fontSize: Int, maxWidth: Int): String {
            val sb = StringBuilder()
            var current = ""

            for (ch in word) {
                val test = current + ch
                val width = measureHtmlTextWidth("<html><div style='font-size:${fontSize}px;'>$test</div></html>")
                if (width > maxWidth) {
                    sb.append(current).append("<br>")
                    current = ch.toString()
                } else {
                    current = test
                }
            }

            if (current.isNotEmpty()) {
                sb.append(current)
            }

            return sb.toString()
        }

        fun insertLineBreaks(text: String, fontSize: Int, maxWidth: Int): String {
            val words = text.split(" ")
            val result = StringBuilder()
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = measureHtmlTextWidth("<html><div style='font-size:${fontSize}px;'>$testLine</div></html>")

                if (width > maxWidth) {
                    if (currentLine.isNotEmpty()) {
                        result.append(currentLine.toString().trim()).append("<br>")
                        currentLine = StringBuilder(word)
                    } else {
                        result.append(breakLongWord(word, fontSize, maxWidth)).append("<br>")
                        currentLine = StringBuilder()
                    }
                } else {
                    if (currentLine.isNotEmpty()) currentLine.append(" ")
                    currentLine.append(word)
                }
            }

            if (currentLine.isNotEmpty()) {
                result.append(currentLine.toString().trim())
            }

            return result.toString()
        }

        var fontSize = baseFontSize
        var html: String
        var width: Int

        fun buildHtml(text: String, fontSize: Int): String {
            return "<div style='font-size:${fontSize}px;'>$text</div>"
        }

        do {
            html = buildHtml(rawText, fontSize)
            width = measureHtmlTextWidth("<html>$html</html>")
            if (width <= maxWidth) break
            fontSize--
        } while (fontSize > minimumFontSize)

        if (width > maxWidth) {
            val wrapped = insertLineBreaks(rawText, fontSize, maxWidth)
            html = buildHtml(wrapped, fontSize)
        }

        return html
    }


    fun setEnabledRecursively(comp: Component, enabled: Boolean, vararg exclusives: Component) {
        if (exclusives.contains(comp))
            return

        comp.isEnabled = enabled
        if (comp is Container)
            for (child in comp.components) setEnabledRecursively(child, enabled, *exclusives)
    }

}