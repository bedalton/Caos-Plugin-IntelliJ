package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.CaosValidatorIllegalArgumentException

enum class CaosScriptVarTokenGroup(val value:String) {
    UNKNOWN("???"),
    VARx("VARx"),
    OBVx("OBVx"),
    VAxx("VAxx"),
    OVxx("OVxx"),
    MVxx("MVxx");

    companion object {
        fun fromValue(value:String) : CaosScriptVarTokenGroup {
            return values().first { it.value == value }
        }
        fun fromText(text:String) : CaosScriptVarTokenGroup {
            val variablePrefix = text
                    .replace("[0-9]".toRegex(), "")
                    .toUpperCase()
                    .trim()
            return when(variablePrefix) {
                "VAR" -> VARx
                "VA" -> VAxx
                "OBV" -> OBVx
                "OV" -> OVxx
                "MV" -> MVxx
                else -> throw CaosValidatorIllegalArgumentException("Invalid variable prefix '$variablePrefix' found for VariableGroup.fromValue()")
            }
        }

    }
}