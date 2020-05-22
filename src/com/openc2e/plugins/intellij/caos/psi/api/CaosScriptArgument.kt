package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.deducer.CaosVar

interface CaosScriptArgument : CaosScriptCompositeElement {
    val index:Int
    val expectedType:CaosExpressionValueType
    fun toCaosVar(): CaosVar
}

enum class CaosExpressionValueType(val value:Int, val simpleName:String) {
    INT(1, "integer"),
    FLOAT(2, "float"),
    TOKEN(3, "token"),
    STRING(4, "string"),
    VARIABLE(6, "variable"),
    COMMAND(8, "command"),
    C1_STRING(9, "[string]"),
    BYTE_STRING(10, "[byte-string]"),
    AGENT(11, "agent"),
    ANY(13, "any"),
    CONDITION(15, "condition"),
    DECIMAL(16, "decimal"),
    ANIMATION(17, "[anim]"),
    HEXADECIMAL(18, "hexadecimal"),
    NULL(0, "NULL"),
    UNKNOWN(-1, "UNKNOWN");

    companion object {
        fun fromSimpleName(simpleName: String) : CaosExpressionValueType {
            return values().firstOrNull { it.simpleName == simpleName } ?: UNKNOWN
        }
    }
}