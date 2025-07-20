package com.redlimerl.mcsrlauncher.gui

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.instance.BasicInstance
import com.redlimerl.mcsrlauncher.gui.layout.WrapLayout
import com.redlimerl.mcsrlauncher.gui.listener.MouseFillListener
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.SwingUtils
import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

class MainMenuGui : MainForm() {

    init {
        title = "${MCSRLauncher.APP_NAME} " + if (MCSRLauncher.IS_DEV_VERSION) "Developement Ver." else ("v" + MCSRLauncher.APP_VERSION)
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(900, 600)
        setLocationRelativeTo(null)
        javaClass.classLoader.getResource("icon.png")?.let { iconImage = ImageIcon(it).image }

        initLauncherMenu()
        initInstanceList()
        initInstanceOption()

        contentPane = mainPanel

        I18n.translateGui(this)
        isVisible = true
    }

    private fun initLauncherMenu() {
        initHeaderButton(accountButton)
        accountButton.text = I18n.translate("account.accounts")
        accountButton.addActionListener {
            AccountListGui(this)
        }

        initHeaderButton(createInstanceButton)
        createInstanceButton.text = I18n.translate("instance.new")
        createInstanceButton.addActionListener {
            CreateInstanceGui(this)
        }

        initHeaderButton(settingsButton)
        settingsButton.text = I18n.translate("text.settings")
        settingsButton.addActionListener {
            LauncherOptionGui(this)
        }

        initHeaderButton(discordButton)
        discordButton.text = I18n.translate("text.join_discord_server")
        discordButton.addActionListener {
            Desktop.getDesktop().browse(URI.create("https://mcsrlauncher.github.io/discord"))
        }

        initHeaderButton(patreonButton)
        patreonButton.text = I18n.translate("text.support_us")
        patreonButton.addActionListener {
            Desktop.getDesktop().browse(URI.create("https://mcsrlauncher.github.io/patreon"))
        }
    }

    private fun initHeaderButton(button: JButton) {
        button.icon = ImageIcon((button.icon as ImageIcon).image.getScaledInstance(24, 24, Image.SCALE_SMOOTH))
        button.addMouseListener(MouseFillListener(button))
    }

    private fun initInstanceList() {
        instanceScrollPane.border = BorderFactory.createEmptyBorder()
        SwingUtils.fasterScroll(instanceScrollPane)
        loadInstanceList()
        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                instanceScrollPane.viewport.view.revalidate()
                instanceScrollPane.viewport.view.repaint()
            }
        })
    }

    private fun initInstanceOption() {
//        bottomField.isVisible = false
    }

    fun loadInstanceList() {
        val containerPanel = JPanel(MigLayout("wrap 1", "[grow, fill]", ""))

        for (group in InstanceManager.instances.keys) {
            createInstanceGroup(containerPanel, group)
            containerPanel.add(JSeparator())
        }
        containerPanel.revalidate()
        containerPanel.repaint()
        instanceScrollPane.viewport.view = containerPanel
    }

    private fun createInstanceGroup(panel: JPanel, group: String) {
        val groupTitle = JLabel(group)
        groupTitle.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        groupTitle.font = groupTitle.font.deriveFont(Font.BOLD, groupTitle.font.size2D + 6F)
        panel.add(groupTitle)

        val groupPanel = JPanel(WrapLayout(FlowLayout.LEFT, 10, 10))
        for (basicInstance in InstanceManager.instances[group]!!) {
            this.createInstanceButton(groupPanel, basicInstance)
        }
        panel.add(groupPanel)
    }

    private fun createInstanceButton(panel: JPanel, instance: BasicInstance) {
        val instanceButton = JButton().apply {
            preferredSize = Dimension(120, 120)
            isContentAreaFilled = true
            horizontalAlignment = SwingConstants.CENTER
            horizontalTextPosition = SwingConstants.CENTER
            verticalTextPosition = SwingConstants.BOTTOM
            icon = ImageIcon(ImageIcon(instance.getIconResource()).image.getScaledInstance(60, 60, Image.SCALE_SMOOTH))
            iconTextGap = 6
            margin = Insets(5, 10, 5, 10)
            isOpaque = false
            isRequestFocusEnabled = false
            text = """<html>
                <div style='text-align:center;'>
                    <div style='font-size: 1.1em; font-weight: bold;'>${instance.displayName}</div>
                    <div>v${instance.minecraftVersion} (${instance.getInstanceType()})</div>
                </div> 
            </html>"""
            if (instance.isRunning()) {
                putClientProperty("JComponent.outline", "warning")
            }
        }

        val popupMenu = this.createInstanceMenu(instance)
        instanceButton.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) popupMenu.show(e.component, e.x, e.y)
            }
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    instance.launchWithDialog()
                }
            }
        })
        panel.add(instanceButton, "gap 10")
    }

    private fun createInstanceMenu(instance: BasicInstance): JPopupMenu {
        val popupMenu = JPopupMenu()

        if (!instance.isRunning()) {
            val launchItem = JMenuItem(I18n.translate("instance.launch")).apply {
                addActionListener { instance.launchWithDialog() }
            }
            popupMenu.add(launchItem)
        } else {
            popupMenu.add(JMenuItem(I18n.translate("instance.kill_process")).apply {
                addActionListener { instance.getProcess()?.exit() }
            })
        }

        popupMenu.add(JMenuItem(I18n.translate("instance.edit")).apply {
            addActionListener {
                InstanceOptionGui(this@MainMenuGui, instance)
            }
        })

        popupMenu.add(JMenuItem(I18n.translate("text.open.dot_minecraft")).apply {
            addActionListener { Desktop.getDesktop().open(instance.getGamePath().toFile().apply { mkdirs() }) }
        })

        popupMenu.add(JMenuItem(I18n.translate("instance.delete")).apply {
            addActionListener {
                val confirm = JOptionPane.showConfirmDialog(this@MainMenuGui, I18n.translate("message.delete_instance_confirm"), I18n.translate("instance.delete"), JOptionPane.OK_CANCEL_OPTION)
                if (confirm == JOptionPane.OK_OPTION) {
                    InstanceManager.deleteInstance(instance)
                }
            }
            isEnabled = !instance.isRunning()
        })

        return popupMenu
    }

}