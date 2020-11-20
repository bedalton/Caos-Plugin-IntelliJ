package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant

val NUMBER_REGEX = "[0-9]+".toRegex()

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