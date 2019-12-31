package com.openc2e.plugins.intellij.caos.def.psi.types

import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.def.stubs.types.CaosDefStubTypes

object CaosDefElementTypeFactory {

    @JvmStatic
    public fun factory(debugName:String): IElementType {
        return when(debugName) {
            "CaosDef_COMMAND_DEF_ELEMENT" -> CaosDefStubTypes.COMMAND_ELEMENT
            "CaosDef_DOC_COMMENT" -> CaosDefStubTypes.DOC_COMMENT
            "CaosDef_PARAMETER" -> CaosDefStubTypes.PARAMETER;
            else->throw IndexOutOfBoundsException("Failed to recognize token type: $debugName")
        }
    }
}