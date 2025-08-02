package com.redlimerl.mcsrlauncher.data.launcher

interface LauncherSharedOptions {
    var javaPath: String
    var jvmArguments: String
    var minMemory: Int
    var maxMemory: Int
    var maximumResolution: Boolean
    var resolutionWidth: Int
    var resolutionHeight: Int
}