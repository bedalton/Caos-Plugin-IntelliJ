package com.openc2e.plugins.intellij.agenteering.caos.psi.api

import com.openc2e.plugins.intellij.agenteering.caos.deducer.CaosVar

interface CaosScriptCommandElement : CaosScriptCompositeElement {
    val commandString:String
    val commandToken:CaosScriptIsCommandToken?
    val arguments:List<CaosScriptArgument>
    val argumentValues:List<CaosVar>
}

val CaosScriptCommandElement.argumentsLength:Int get() = argumentValues.size