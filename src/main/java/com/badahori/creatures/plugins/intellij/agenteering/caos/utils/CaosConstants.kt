package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant

val NUMBER_REGEX = "\\d+".toRegex()
val NEG_NUMBER_REGEX = "-?\\d+".toRegex()
val POS_NEG_NUMBER_REGEX = "[-+]?\\d+".toRegex()

object CaosConstants {
    val VARIANTS = listOf(
            CaosVariant.C1,
            CaosVariant.C2,
            CaosVariant.CV,
            CaosVariant.C3,
            CaosVariant.DS,
            CaosVariant.SM
    )

}