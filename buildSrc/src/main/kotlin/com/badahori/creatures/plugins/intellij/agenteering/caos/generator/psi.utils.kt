package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

import java.util.logging.Logger

const val UNDEF = "{UNDEF}"

internal fun String?.nullIfUndefOrBlank(): String? {
    return if (this == null || this == UNDEF || this.isBlank())
        null
    else
        this
}

internal val LOGGER by lazy {
    Logger.getLogger("#CAOS")
}