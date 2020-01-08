package com.openc2e.plugins.intellij.caos.def.stubs.types

object CaosDefStubTypes {

    @JvmStatic
    val TYPE_DEFINITION_VALUE: CaosDefTypeDefValueStubType = CaosDefTypeDefValueStubType("CaosDef_TYPE_DEFINITION")
    @JvmStatic
    val TYPE_DEFINITION_ELEMENT: CaosDefTypeDefinitionStubType = CaosDefTypeDefinitionStubType("CaosDef_TYPE_DEFINITION_ELEMENT")
    @JvmStatic
    val COMMAND_ELEMENT = CaosDefCommandElementStubType("CaosDef_COMMAND_DEF_ELEMENT")
    @JvmStatic
    val DOC_COMMENT = CaosDefCommentElementStubType("CaosDef_DOC_COMMENT")
    @JvmStatic
    val PARAMETER = CaosDefParameterStubType("CaosDef_PARAMETER")
    @JvmStatic
    val FILE:CaosDefFileStubType = CaosDefFileStubType()
}