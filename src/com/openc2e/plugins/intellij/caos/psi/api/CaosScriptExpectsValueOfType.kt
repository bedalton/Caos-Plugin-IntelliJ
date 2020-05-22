package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement

interface CaosScriptExpectsValueOfType : CaosScriptCompositeElement, CaosScriptArgument {
    val rvalue:CaosScriptRvalue?
}