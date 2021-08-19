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
                else -> throw Exception("Invalid variable prefix '$variablePrefix' found for VariableGroup.fromValue()")
            }
        }

    }
}

infix fun CaosScriptVarTokenGroup.like(other:CaosScriptVarTokenGroup) : Boolean {
    if (this == other)
        return true
    if (isVAxxLike)
        return other.isVAxxLike
    if (isOVxxLike)
        return other.isOVxxLike
    // MVxx has not analogous type, so if they do not match at start, they do not match
    return false
}

val CaosScriptVarTokenGroup.isVAxxLike:Boolean get() = this == CaosScriptVarTokenGroup.VARx || this == CaosScriptVarTokenGroup.VAxx
val CaosScriptVarTokenGroup.isOVxxLike:Boolean get() = this == CaosScriptVarTokenGroup.OBVx || this == CaosScriptVarTokenGroup.OVxx
val CaosScriptVarTokenGroup.isMVxxLike:Boolean get() = this == CaosScriptVarTokenGroup.MVxx