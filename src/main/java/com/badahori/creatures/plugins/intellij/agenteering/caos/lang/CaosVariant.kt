package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import icons.CaosScriptIcons
import javax.swing.Icon

sealed class CaosVariant(open val code: String, open val fullName: String, open val index: Int, open val icon: Icon) {
    object C1 : CaosVariant("C1", "Creatures 1", 1, CaosScriptIcons.C1)
    object C2 : CaosVariant("C2", "Creatures 2", 2, CaosScriptIcons.C2)
    object CV : CaosVariant("CV", "Creatures Village", 3, CaosScriptIcons.CV)
    object C3 : CaosVariant("C3", "Creatures 3", 4, CaosScriptIcons.C3)
    object DS : CaosVariant("DS", "Docking Station", 5, CaosScriptIcons.DS)
    object SM : CaosVariant("SM", "Sea Monkeys", 6, CaosScriptIcons.SM)
    object UNKNOWN : CaosVariant("??", "Unknown", -1, CaosScriptIcons.MODULE_ICON)
    data class OTHER internal constructor(
            override val code: String,
            override val fullName: String,
            override val icon: Icon = CaosScriptIcons.MODULE_ICON
    ) : CaosVariant(code, fullName, -1, icon) {
        init {
            others.add(this)
        }
    }
    companion object {
        private val others:MutableList<CaosVariant> = mutableListOf()
        fun fromVal(variant: String): CaosVariant {
            return when (variant) {
                "C1" -> C1
                "C2" -> C2
                "CV" -> CV
                "C3" -> C3
                "DS" -> DS
                "SM" -> SM
                else -> others.firstOrNull { it.code == variant } ?: UNKNOWN
            }
        }

        fun registerVariant(
                code: String,
                fullName: String,
                icon: Icon = CaosScriptIcons.MODULE_ICON
        ) : CaosVariant {
            val existing = fromVal(code)
            if (existing != UNKNOWN)
                return existing
            return OTHER(code, fullName, icon)
        }
    }

    val isOld: Boolean
        get() {
            return this in VARIANT_OLD
        }
    val isNotOld: Boolean
        get() {
            return this !in VARIANT_OLD
        }
}

fun CaosVariant?.orDefault(): CaosVariant {
    if (this != null)
        return this
    return CaosScriptProjectSettings.variant
}

val VARIANT_OLD = listOf(CaosVariant.C1, CaosVariant.C2)