package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStruct

interface CaosScriptExpectsValueOfType : CaosScriptCompositeElement, CaosScriptArgument {
    val rvalue: CaosScriptRvalueLike?
    val parameterListValue:CaosDefTypeDefValueStruct?
}