package com.openc2e.plugins.intellij.agenteering.caos.def.psi.types

import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubTypes

object CaosDefElementTypeFactory {

    @JvmStatic
    public fun factory(debugName:String): IElementType {
        return when(debugName) {
            "CaosDef_COMMAND_DEF_ELEMENT" -> CaosDefStubTypes.COMMAND_ELEMENT
            "CaosDef_DOC_COMMENT" -> CaosDefStubTypes.DOC_COMMENT
            "CaosDef_PARAMETER" -> CaosDefStubTypes.PARAMETER;
            "CaosDef_TYPE_DEFINITION_ELEMENT" -> CaosDefStubTypes.TYPE_DEFINITION_ELEMENT
            "CaosDef_TYPE_DEFINITION" -> CaosDefStubTypes.TYPE_DEFINITION_VALUE
            "CaosDef_DOC_COMMENT_HASHTAG" -> CaosDefStubTypes.HASHTAG
            else->throw IndexOutOfBoundsException("Failed to recognize token type: $debugName")
        }
    }
}