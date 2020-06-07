package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiNamedElement
import com.openc2e.plugins.intellij.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference

interface CaosScriptIsCommandToken : PsiNamedElement, CaosScriptCompositeElement, CaosScriptShouldBeLowerCase {
    fun isVariant(variants:List<CaosVariant>, strict:Boolean) : Boolean
    val reference:CaosScriptCommandTokenReference
    val commandString:String
}

val CaosScriptIsCommandToken.variant get() = containingCaosFile.variant