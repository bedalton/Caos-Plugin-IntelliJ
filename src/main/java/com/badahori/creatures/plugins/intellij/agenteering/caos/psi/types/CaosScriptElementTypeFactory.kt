package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes
import com.intellij.psi.tree.IElementType

class CaosScriptElementTypeFactory {

    companion object {
        @JvmStatic
        fun factory(debugName:String) : IElementType {
            return when (debugName) {
                "CaosScript_SUBROUTINE" -> CaosScriptStubTypes.SUBROUTINE
                "CaosScript_COMMAND_CALL" -> CaosScriptStubTypes.COMMAND_CALL
                "CaosScript_VAR_TOKEN" -> CaosScriptStubTypes.VAR_TOKEN
                "CaosScript_RVALUE" -> CaosScriptStubTypes.RVALUE
                "CaosScript_TOKEN_RVALUE" -> CaosScriptStubTypes.TOKEN_RVALUE
                "CaosScript_RVALUE_PRIME" -> CaosScriptStubTypes.RVALUE_PRIME
                "CaosScript_LVALUE" -> CaosScriptStubTypes.LVALUE
                "CaosScript_C_ASSIGNMENT" -> CaosScriptStubTypes.VAR_ASSIGNMENT
                "CaosScript_C_TARG" -> CaosScriptStubTypes.TARG_ASSIGNMENT
                "CaosScript_NAMED_GAME_VAR" -> CaosScriptStubTypes.NAMED_GAME_VAR
                "CaosScript_EVENT_SCRIPT" -> CaosScriptStubTypes.EVENT_SCRIPT
                "CaosScript_MACRO" -> CaosScriptStubTypes.MACRO
                "CaosScript_REMOVAL_SCRIPT" -> CaosScriptStubTypes.REMOVAL_SCRIPT
                "CaosScript_INSTALL_SCRIPT" -> CaosScriptStubTypes.INSTALL_SCRIPT
                "CaosScript_C_RNDV" -> CaosScriptStubTypes.RNDV
                "CaosScript_CAOS_2_TAG" -> CaosScriptStubTypes.CAOS_2_TAG
                "CaosScript_CAOS_2_COMMAND" -> CaosScriptStubTypes.CAOS_2_COMMAND
                "CaosScript_CAOS_2_BLOCK" -> CaosScriptStubTypes.CAOS_2_BLOCK
                "CaosScript_SUBROUTINE_NAME" -> CaosScriptStubTypes.SUBROUTINE_NAME
                "CaosScript_QUOTE_STRING_LITERAL" -> CaosScriptStubTypes.QUOTE_STRING_LITERAL
                else -> throw IndexOutOfBoundsException("Caos token '$debugName' is not recognized")
            }
        }
    }
}