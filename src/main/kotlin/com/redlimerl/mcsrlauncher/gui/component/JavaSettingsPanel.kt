package com.redlimerl.mcsrlauncher.gui.component

import com.redlimerl.mcsrlauncher.data.launcher.LauncherSharedOptions
import com.redlimerl.mcsrlauncher.gui.JavaManagerGui
import com.redlimerl.mcsrlauncher.gui.components.AbstractJavaSettingsPanel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JDialog
import javax.swing.SpinnerNumberModel
import kotlin.math.max
import kotlin.math.min

class JavaSettingsPanel(parent: JDialog, val options: LauncherSharedOptions, private val onUpdate: () -> Unit) : AbstractJavaSettingsPanel() {

    init {
        layout = BorderLayout()
        add(this.rootPanel, BorderLayout.CENTER)

        fun saveJavaPath() {
            options.javaPath = this.javaPathField.text
            onUpdate()
        }
        this.javaPathField.addActionListener { saveJavaPath() }
        this.javaPathField.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {}
            override fun focusLost(e: FocusEvent?) {
                saveJavaPath()
            }
        })

        fun refreshJavaPath() {
            this.javaPathField.text = options.javaPath
        }

        refreshJavaPath()

        this.javaChangeButton.addActionListener {
            JavaManagerGui(parent, options.javaPath) {
                options.javaPath = it
                onUpdate()
                refreshJavaPath()
            }
        }


        this.javaMinMemorySlider.value = options.minMemory
        this.javaMinMemorySpinner.model = SpinnerNumberModel(options.minMemory, 512, 8096 * 2 + 1, 512)
        this.javaMaxMemorySlider.value = options.maxMemory
        this.javaMaxMemorySpinner.model = SpinnerNumberModel(options.maxMemory, 512, 8096 * 2 + 1, 512)
        this.javaMinMemorySlider.addChangeListener {
            this.javaMinMemorySlider.value = min(this.javaMinMemorySlider.value, options.maxMemory)
            options.minMemory = this.javaMinMemorySlider.value
            this.javaMinMemorySpinner.value = options.minMemory
            onUpdate()
        }
        this.javaMinMemorySpinner.addChangeListener {
            this.javaMinMemorySpinner.value = min(this.javaMinMemorySpinner.value as Int, options.maxMemory)
            options.minMemory = this.javaMinMemorySpinner.value as Int
            this.javaMinMemorySlider.value = options.minMemory
            onUpdate()
        }
        this.javaMaxMemorySlider.addChangeListener {
            this.javaMaxMemorySlider.value = max(this.javaMaxMemorySlider.value, options.minMemory)
            options.maxMemory = this.javaMaxMemorySlider.value
            this.javaMaxMemorySpinner.value = options.maxMemory
            onUpdate()
        }
        this.javaMaxMemorySpinner.addChangeListener {
            this.javaMaxMemorySpinner.value = max(this.javaMaxMemorySpinner.value as Int, options.minMemory)
            options.maxMemory = this.javaMaxMemorySpinner.value as Int
            this.javaMaxMemorySlider.value = options.maxMemory
            onUpdate()
        }


        this.jvmArgumentArea.text = options.jvmArguments
        this.jvmArgumentArea.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {}
            override fun focusLost(e: FocusEvent?) {
                options.jvmArguments = jvmArgumentArea.text
                onUpdate()
            }
        })
    }

    fun setAllEnabled(enabled: Boolean) {
        setEnabledRecursively(this, enabled)
    }

    private fun setEnabledRecursively(component: Component, enabled: Boolean) {
        component.isEnabled = enabled
        if (component is Container) {
            for (child in component.components) {
                setEnabledRecursively(child, enabled)
            }
        }
    }
}