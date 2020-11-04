package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubTypes
import com.intellij.psi.tree.IElementType

object CaosDefElementTypeFactory {

    @JvmStatic
    fun factory(debugName:String): IElementType {
        return when(debugName) {
            "CaosDef_COMMAND_DEF_ELEMENT" -> CaosDefStubTypes.COMMAND_ELEMENT
            "CaosDef_DOC_COMMENT" -> CaosDefStubTypes.DOC_COMMENT
            "CaosDef_PARAMETER" -> CaosDefStubTypes.PARAMETER
            "CaosDef_VALUES_LIST_ELEMENT" -> CaosDefStubTypes.VALUES_LIST_ELEMENT
            "CaosDef_VALUES_LIST_VALUE" -> CaosDefStubTypes.VALUES_LIST_VALUE
            "CaosDef_DOC_COMMENT_HASHTAG" -> CaosDefStubTypes.HASHTAG
            else->throw IndexOutOfBoundsException("Failed to recognize token type: $debugName")
        }
    }
}