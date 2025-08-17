package com.redlimerl.mcsrlauncher.gui.component

import com.redlimerl.mcsrlauncher.launcher.AccountManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.awt.*
import javax.swing.*


class AccountListComponent : JPanel() {
    private val accountListener: ArrayList<Runnable> = arrayListOf()

    init {
        layout = GridBagLayout()
        loadAll()
    }

    private fun updateAccountStatus() {
        for (runnable in accountListener) runnable.run()
    }

    fun loadAll() {
        removeAll()
        revalidate()
        repaint()
        accountListener.clear()

        layout = GridBagLayout()
        val grid = GridBagConstraints().apply {
            gridy = 0
            insets = Insets(5, 5, 5, 5)
            anchor = GridBagConstraints.WEST
        }

        for (account in AccountManager.getAllAccounts()) {
            grid.gridx = 0
            grid.weightx = 1.0
            grid.fill = GridBagConstraints.HORIZONTAL
            val nicknameLabel = JLabel(account.profile.nickname)
            add(nicknameLabel, grid)
            object : LauncherWorker() {
                override fun work(dialog: JDialog) {
                    nicknameLabel.icon = AccountManager.getSkinHead(account, nicknameLabel.font.size)
                }
            }.start()

            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
                val selectAccountButton = JButton(I18n.translate("account.select"))
                val refreshTokenButton = JButton(I18n.translate("account.refresh"))
                val removeAccountButton = JButton(I18n.translate("account.remove"))

                add(selectAccountButton)
                add(refreshTokenButton)
                add(removeAccountButton)

                selectAccountButton.addActionListener {
                    AccountManager.setActiveAccount(account)
                    updateAccountStatus()
                }

                refreshTokenButton.addActionListener {
                    if (System.currentTimeMillis() - account.getLastRefreshTime() < 60 * 1000) {
                        JOptionPane.showMessageDialog(parent, I18n.translate("message.refresh_cooldown_warning", 60), I18n.translate("text.error"), JOptionPane.OK_OPTION)
                        return@addActionListener
                    }

                    refreshTokenButton.isEnabled = false
                    object : LauncherWorker() {
                        override fun work(dialog: JDialog) {
                            account.profile.refresh(this, account, true)
                            AccountManager.save()
                            nicknameLabel.icon = AccountManager.getSkinHead(account, nicknameLabel.font.size)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            refreshTokenButton.isEnabled = true
                        }
                    }.showDialog().start()
                }

                removeAccountButton.addActionListener {
                    AccountManager.removeAccount(account)
                    SwingUtilities.invokeLater { loadAll() }
                }

                accountListener.add {
                    nicknameLabel.font = nicknameLabel.font.let {
                        Font(it.name, if (account == AccountManager.getActiveAccount()) Font.BOLD else Font.PLAIN, it.size)
                    }
                    selectAccountButton.isEnabled = account != AccountManager.getActiveAccount()
                }
            }

            grid.gridx = 1
            grid.weightx = 0.0
            grid.fill = GridBagConstraints.NONE
            grid.anchor = GridBagConstraints.EAST
            add(buttonPanel, grid)

            grid.gridy++
        }

        val spacer = GridBagConstraints().apply {
            gridx = 0
            gridy = grid.gridy
            weighty = 1.0
            fill = GridBagConstraints.VERTICAL
            gridwidth = GridBagConstraints.REMAINDER
        }
        add(Box.createVerticalGlue(), spacer)

        updateAccountStatus()
        updateUI()
    }

}