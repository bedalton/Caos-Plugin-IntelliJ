package com.openc2e.plugins.intellij.caos.utils

import com.openc2e.plugins.intellij.caos.lang.CaosVariant


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