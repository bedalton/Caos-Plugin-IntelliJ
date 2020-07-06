package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.intellij.psi.PsiNamedElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.CaosScriptCommandTokenReference
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.equalsIgnoreCase

interface CaosScriptIsCommandToken : PsiNamedElement, CaosScriptCompositeElement, CaosScriptShouldBeLowerCase {
    fun isVariant(variants:List<CaosVariant>, strict:Boolean) : Boolean
    val reference:CaosScriptCommandTokenReference
    val commandString:String
}

val CaosScriptIsCommandToken.variant get() = containingCaosFile.variant

infix fun CaosScriptIsCommandToken?.equalTo(otherValue:String) : Boolean = this?.text?.equalsIgnoreCase(otherValue) ?: false
infix fun CaosScriptIsCommandToken?.notEqualTo(otherValue:String) : Boolean = this?.text?.equalsIgnoreCase(otherValue) ?: false