package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import icons.CaosScriptIcons
import javax.swing.Icon

enum class CaosVariant(val code:String, val fullName:String, val index:Int, val icon:Icon) {
    C1("C1", "Creatures 1", 1, CaosScriptIcons.C1),
    C2("C2", "Creatures 2", 2, CaosScriptIcons.C2),
    CV("CV", "Creatures Village", 3, CaosScriptIcons.CV),
    C3("C3", "Creatures 3", 4, CaosScriptIcons.C3),
    DS("DS", "Docking Station", 5, CaosScriptIcons.DS),
    SM("SM", "Sea Monkeys", 6, CaosScriptIcons.SM),
    UNKNOWN("??", "Unknown", -1, CaosScriptIcons.MODULE_ICON);

    companion object {
        fun fromVal(variant:String) :CaosVariant {
            return when (variant) {
                "C1" -> C1
                "C2" -> C2
                "CV" -> CV
                "C3" -> C3
                "DS" -> DS
                "SM" -> SM
                else -> UNKNOWN
            }
        }
    }

    val isOld:Boolean get() {
        return this in VARIANT_OLD
    }
    val isNotOld:Boolean get() {
        return this !in VARIANT_OLD
    }
}

fun CaosVariant?.orDefault() : CaosVariant {
    if (this != null)
        return this
    return CaosScriptProjectSettings.variant
}

val VARIANT_OLD = listOf(CaosVariant.C1, CaosVariant.C2)