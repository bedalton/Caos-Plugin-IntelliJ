package com.openc2e.plugins.intellij.agenteering.caos.psi.api

import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStruct

interface CaosScriptExpectsValueOfType : CaosScriptCompositeElement, CaosScriptArgument {
    val rvalue:CaosScriptRvalue?
    val parameterListValue:CaosDefTypeDefValueStruct?
}