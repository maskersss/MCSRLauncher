package com.redlimerl.mcsrlauncher.instance.mod

import com.redlimerl.mcsrlauncher.util.I18n

enum class ModDownloadMethod {
    DOWNLOAD_RECOMMENDS,
    DELETE_ALL_DOWNLOAD,
    UPDATE_EXISTING_MODS;

    override fun toString(): String {
        return I18n.translate("download_method.${name.lowercase()}")
    }
}