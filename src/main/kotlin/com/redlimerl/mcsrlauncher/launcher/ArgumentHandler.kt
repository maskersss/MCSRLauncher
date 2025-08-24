package com.redlimerl.mcsrlauncher.launcher

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option

class ArgumentHandler : CliktCommand() {

    val launch by option("--launch", "-l").multiple()
    val account by option("--account", "-a")

    override fun run() {
        if (account != null) AccountManager.getAllAccounts().find { it.profile.nickname == account }?.let {
            AccountManager.setActiveAccount(it)
        }
        for (instanceName in launch) InstanceManager.getInstance(instanceName)?.launchWithDialog()
    }

}