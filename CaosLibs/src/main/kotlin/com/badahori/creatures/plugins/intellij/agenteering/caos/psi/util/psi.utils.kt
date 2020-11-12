package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import java.util.logging.Logger

const val UNDEF = "{UNDEF}"

fun String?.nullIfUndefOrBlank(): String? {
    return if (this == null || this == UNDEF || this.isBlank())
        null
    else
        this
}

val LOGGER by lazy {
    Logger.getLogger("#CAOS")
}