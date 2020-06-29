package com.openc2e.plugins.intellij.agenteering.caos.psi.api

import com.intellij.psi.PsiNamedElement
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.variant
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.agenteering.caos.references.CaosScriptCommandTokenReference

interface CaosScriptIsCommandToken : PsiNamedElement, CaosScriptCompositeElement, CaosScriptShouldBeLowerCase {
    fun isVariant(variants:List<CaosVariant>, strict:Boolean) : Boolean
    val reference:CaosScriptCommandTokenReference
    val commandString:String
}

val CaosScriptIsCommandToken.variant get() = containingCaosFile.variant