package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.intellij.psi.tree.IElementType

/**
 * Represents as CAOS command with arguments if any
 */
interface CaosScriptCommandElement : CaosScriptCompositeElement, CaosScriptCommandLike {
    val arguments:List<CaosScriptArgument>
    val argumentValues:List<CaosExpressionValueType>
    val commandDefinition:CaosCommand?
}

val CaosScriptCommandElement.argumentsLength:Int get() = argumentValues.size
val CaosScriptCommandElement.commandStringUpper:String? get() = commandString?.uppercase()