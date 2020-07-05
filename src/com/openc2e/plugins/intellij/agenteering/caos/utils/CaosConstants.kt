package com.openc2e.plugins.intellij.agenteering.caos.utils

import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant

val NUMBER_REGEX = "[0-9]+".toRegex();

object CaosConstants {
    val BASE_TYPES = listOf(
            "Object",
            "Boolean",
            "String",
            "Integer",
            "LongInteger",
            "Double",
            "Float",
            "Function",
            "Dynamic"
    )

    val VARIANTS = listOf(
            CaosVariant.C1,
            CaosVariant.C2,
            CaosVariant.CV,
            CaosVariant.C3,
            CaosVariant.DS,
            CaosVariant.SM
    )

}