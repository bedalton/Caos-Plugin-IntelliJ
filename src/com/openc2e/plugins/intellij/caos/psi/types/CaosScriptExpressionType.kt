package com.openc2e.plugins.intellij.caos.psi.types

enum class CaosScriptExpressionType(val value:String) {
    INT("integer"),
    STRING("string"),
    TOKEN("token"),
    FLOAT("float"),
    BRACKET_STRING("[string]"),
    VARIABLE("variable"),
    EQ("EqOp"),
    COMMAND("command");

    companion object {
        fun fromValue(value:String) : CaosScriptExpressionType {
            return values().first{
                it.value == value
            }
        }
    }
}