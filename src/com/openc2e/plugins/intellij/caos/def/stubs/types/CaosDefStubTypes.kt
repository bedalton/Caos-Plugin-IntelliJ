package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFileType

object CaosDefStubTypes {

    @JvmStatic
    val COMMAND_ELEMENT = CaosDefCommandElementStubType("CaosDef_COMMAND_DEF_ELEMENT")
    @JvmStatic
    val FILE:CaosDefFileStubType = CaosDefFileStubType()
}