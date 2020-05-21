package com.openc2e.plugins.intellij.caos.psi.types

import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.stubs.types.CaosScriptStubTypes

public class CaosScriptElementTypeFactory {

    companion object {
        @JvmStatic
        fun factory(debugName:String) : IElementType {
            return when (debugName) {
                "CaosScript_SUBROUTINE" -> CaosScriptStubTypes.SUBROUTINE
                "CaosScript_COMMAND_CALL" -> CaosScriptStubTypes.COMMAND_CALL
                "CaosScript_VAR_TOKEN" -> CaosScriptStubTypes.VAR_TOKEN
                "CaosScript_RVALUE" -> CaosScriptStubTypes.RVALUE
                "CaosScript_LVALUE" -> CaosScriptStubTypes.LVALUE
                "CaosScript_C_ASSIGNMENT" -> CaosScriptStubTypes.VAR_ASSIGNMENT
                "CaosScript_NAMED_VAR" -> CaosScriptStubTypes.NAMED_VAR
                "CaosScript_NAMED_CONSTANT" -> CaosScriptStubTypes.NAMED_CONSTANT
                "CaosScript_CONSTANT_ASSIGNMENT" -> CaosScriptStubTypes.CONSTANT_ASSIGNMENT
                "CaosScript_C_TARG" -> CaosScriptStubTypes.TARG_ASSIGNMENT
                "CaosScript_EXPECTS_INT" -> CaosScriptStubTypes.EXPECTS_INT
                "CaosScript_EXPECTS_STRING" -> CaosScriptStubTypes.EXPECTS_STRING
                "CaosScript_EXPECTS_FLOAT" -> CaosScriptStubTypes.EXPECTS_FLOAT
                "CaosScript_EXPECTS_AGENT" -> CaosScriptStubTypes.EXPECTS_AGENT
                "CaosScript_EXPECTS_VALUE" -> CaosScriptStubTypes.EXPECTS_VALUE
                "CaosScript_EXPECTS_TOKEN" -> CaosScriptStubTypes.EXPECTS_TOKEN
                "CaosScript_EXPECTS_C_1_STRING" -> CaosScriptStubTypes.EXPECTS_C1_STRING
                "CaosScript_EXPECTS_BYTE_STRING" -> CaosScriptStubTypes.EXPECTS_BYTE_STRING
                "CaosScript_EXPECTS_DECIMAL" -> CaosScriptStubTypes.EXPECTS_DECIMAL
                "CaosScript_NAMED_GAME_VAR" -> CaosScriptStubTypes.NAMED_GAME_VAR
                "CaosScript_EVENT_SCRIPT" -> CaosScriptStubTypes.EVENT_SCRIPT
                "CaosScript_MACRO" -> CaosScriptStubTypes.MACRO
                "CaosScript_NAMED_VAR_ASSIGNMENT" -> CaosScriptStubTypes.NAMED_VAR_ASSIGNMENT
                else -> throw IndexOutOfBoundsException("Caos token '$debugName' is not recognized")
            }
        }
    }
}