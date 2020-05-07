package com.openc2e.plugins.intellij.caos.psi.types

import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.stubs.types.CaosScriptStubTypes

public class CaosScriptElementTypeFactory {

    companion object {
        @JvmStatic
        fun factory(debugName:String) : IElementType {
            return when (debugName) {
                //"CaosScript_COMMAND" -> CaosScriptStubTypes.COMMAND
                "CaosScript_SUBROUTINE" -> CaosScriptStubTypes.SUBROUTINE
                "CaosScript_COMMAND_CALL" -> CaosScriptStubTypes.COMMAND_CALL
                "CaosScript_EXPRESSION" -> CaosScriptStubTypes.EXPRESSION
                "CaosScript_VAR_TOKEN" -> CaosScriptStubTypes.VAR_TOKEN
                else -> throw IndexOutOfBoundsException("Caos token '$debugName' is not recognized")
            }
        }
    }
}