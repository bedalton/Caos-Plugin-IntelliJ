package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand

interface CaosScriptCommandElement : CaosScriptCompositeElement {
    val commandString:String
    val commandToken:CaosScriptIsCommandToken?
    val arguments:List<CaosScriptArgument>
    val argumentValues:List<CaosVar>
    val commandDefinition:CaosCommand?
}

val CaosScriptCommandElement.argumentsLength:Int get() = argumentValues.size