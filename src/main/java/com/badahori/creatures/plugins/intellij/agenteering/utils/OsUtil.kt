package com.badahori.creatures.plugins.intellij.agenteering.utils

object OsUtil {
    private val osString by lazy { System.getProperty("os.name").toLowerCase() }
    val isWindows by lazy {
        osString.contains("win")
    }
    val isLinux by lazy {
        osString.contains("nix") || osString.contains("nux") || osString.contains("aix")
    }
    val isMac by lazy {
        osString.contains("mac")
    }
}