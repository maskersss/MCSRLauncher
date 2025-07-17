package com.redlimerl.mcsrlauncher.util

import javax.swing.JScrollPane

object SwingUtils {

    fun fasterScroll(scrollPane: JScrollPane) {
        scrollPane.horizontalScrollBar.unitIncrement *= 15
        scrollPane.verticalScrollBar.unitIncrement *= 15
    }

}