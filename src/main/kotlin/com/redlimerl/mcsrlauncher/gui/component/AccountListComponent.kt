package com.redlimerl.mcsrlauncher.gui.component

import com.redlimerl.mcsrlauncher.launcher.AccountManager
import com.redlimerl.mcsrlauncher.util.I18n
import com.redlimerl.mcsrlauncher.util.LauncherWorker
import java.awt.*
import javax.swing.*


class AccountListComponent(private val parent: Window) : JPanel() {
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
        accountListener.clear()

        val grid = GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, Insets(5, 3, 5, 3), 0, 0)

        for (account in AccountManager.getAllAccounts()) {
            grid.gridx = 0
            grid.weightx = 1.0
            val nicknameLabel = JLabel(account.profile.nickname)
            add(nicknameLabel, grid)

            grid.gridx = 2
            grid.weightx = 0.0
            val selectAccountButton = JButton(I18n.translate("account.select"))
            selectAccountButton.addActionListener {
                AccountManager.setActiveAccount(account)
                updateAccountStatus()
            }
            add(selectAccountButton, grid)
            grid.gridx++

            val refreshTokenButton = JButton(I18n.translate("account.refresh"))
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
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        refreshTokenButton.isEnabled = true
                    }
                }.start()
            }
            add(refreshTokenButton, grid)
            grid.gridx++

            val removeAccountButton = JButton(I18n.translate("account.remove"))
            removeAccountButton.addActionListener {
                AccountManager.removeAccount(account)
                SwingUtilities.invokeLater { loadAll() }
            }
            add(removeAccountButton, grid)
            grid.gridx++

            grid.gridy++

            accountListener.add {
                nicknameLabel.font = nicknameLabel.font.let { Font(it.name, if (account == AccountManager.getActiveAccount()) Font.BOLD else Font.PLAIN, it.size) }
                selectAccountButton.isEnabled = account != AccountManager.getActiveAccount()
            }
        }

        updateAccountStatus()
        updateUI()
    }
}