package com.redlimerl.mcsrlauncher.launcher

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.MCSRLauncher.JSON
import com.redlimerl.mcsrlauncher.data.MicrosoftAccount
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.apache.commons.io.FileUtils
import java.nio.file.Path

object AccountManager {

    private var activeIndex: Int = -1
    private val accounts: ArrayList<MicrosoftAccount> = arrayListOf()
    val path: Path = MCSRLauncher.BASE_PATH.resolve("accounts.json")

    fun addAccount(newAccount: MicrosoftAccount) {
        for (account in accounts) {
            if (account.profile.uuid == newAccount.profile.uuid) {
                accounts.remove(account)
                break
            }
        }
        accounts.add(newAccount)
        activeIndex = accounts.indexOf(newAccount)
        save()
    }

    fun removeAccount(account: MicrosoftAccount) {
        if (getActiveAccount() == account) {
            activeIndex = accounts.size - 2
        }
        accounts.remove(account)
        save()
    }

    fun getActiveAccount(): MicrosoftAccount? {
        if (accounts.isNotEmpty() && activeIndex in 0..accounts.size) {
            return accounts[activeIndex]
        }
        return null
    }

    fun setActiveAccount(account: MicrosoftAccount) {
        activeIndex = accounts.indexOf(account)
        save()
    }

    fun getAllAccounts(): List<MicrosoftAccount> {
        return accounts
    }

    fun load() {
        if (!path.toFile().exists()) {
            save()
            return
        }

        val json = JSON.parseToJsonElement(FileUtils.readFileToString(path.toFile(), Charsets.UTF_8)).jsonObject
        activeIndex = json["active"]!!.jsonPrimitive.int
        for (jsonElement in json["accounts"]!!.jsonArray) {
            accounts.add(JSON.decodeFromJsonElement(jsonElement))
        }
    }

    fun save() {
        val json = JsonObject(mapOf(
            Pair("active", JsonPrimitive(activeIndex)),
            Pair("accounts", JsonArray(accounts.map { JSON.encodeToJsonElement(it) }))
        ))

        FileUtils.writeStringToFile(path.toFile(), JSON.encodeToString(json), Charsets.UTF_8)
    }
}