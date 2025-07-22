package com.redlimerl.mcsrlauncher.instance.mod

import com.redlimerl.mcsrlauncher.util.I18n

enum class ModCategory {
    RANDOM_SEED,
    SET_SEED;

    override fun toString(): String {
        return I18n.translate("mod.category.${name.lowercase()}")
    }
}