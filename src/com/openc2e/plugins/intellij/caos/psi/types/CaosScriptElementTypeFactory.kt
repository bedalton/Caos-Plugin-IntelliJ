package com.openc2e.plugins.intellij.caos.psi.types

import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.stubs.types.CaosScriptStubTypes

public class CaosScriptElementTypeFactory {

    companion object {
        @JvmStatic
        fun factory(debugName:String) : IElementType {
            return when (debugName) {
                "CaosScript_COMMAND" -> CaosScriptStubTypes.COMMAND
                "CaosScript_COMMAND_CALL" -> CaosScriptStubTypes.COMMAND_CALL
                "CaosScript_COMMAND_TOKEN" -> CaosScriptStubTypes.COMMAND_TOKEN
                "CaosScript_EXPRESSION" -> CaosScriptStubTypes.EXPRESSION
                else -> throw IndexOutOfBoundsException("Caos token '$debugName' is not recognized")
            }
        }
    }
}