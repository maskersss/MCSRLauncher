package com.redlimerl.mcsrlauncher.launcher

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.MCSRLauncher.JSON
import com.redlimerl.mcsrlauncher.data.MicrosoftAccount
import kotlinx.serialization.json.*
import org.apache.commons.io.FileUtils
import java.awt.Image
import java.net.URL
import java.nio.file.Path
import javax.swing.ImageIcon

object AccountManager {

    private var activeIndex: Int = -1
    private val accounts: ArrayList<MicrosoftAccount> = arrayListOf()
    private val cachedSkinHead: HashMap<MicrosoftAccount, Image> = hashMapOf()

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

    fun getSkinHead(account: MicrosoftAccount, size: Int): ImageIcon {
        return if (MCSRLauncher.options.skinHead3d) ImageIcon(cachedSkinHead.computeIfAbsent(account) {
                ImageIcon(URL("https://mc-heads.net/head/${account.profile.uuid}")).image
            }.getScaledInstance(size, (size * 1.15f).toInt(), Image.SCALE_SMOOTH))
        else ImageIcon(cachedSkinHead.computeIfAbsent(account) {
            ImageIcon(URL("https://mc-heads.net/avatar/${account.profile.uuid}")).image
        }.getScaledInstance(size, size, Image.SCALE_SMOOTH))
    }

    fun clearSkinHeadCache() {
        cachedSkinHead.clear()
    }
}