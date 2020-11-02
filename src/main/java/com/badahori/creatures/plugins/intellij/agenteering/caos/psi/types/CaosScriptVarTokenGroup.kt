package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types

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

    }
}