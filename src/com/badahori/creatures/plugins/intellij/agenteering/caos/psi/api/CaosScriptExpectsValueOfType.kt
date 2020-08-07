package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStruct

interface CaosScriptExpectsValueOfType : CaosScriptCompositeElement, CaosScriptArgument {
    val rvalue: CaosScriptRvalueLike?
    val parameterListValue:CaosDefValuesListValueStruct?
}