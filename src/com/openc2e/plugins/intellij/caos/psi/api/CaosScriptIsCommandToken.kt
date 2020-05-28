package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiNamedElement
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference

interface CaosScriptIsCommandToken : PsiNamedElement, CaosScriptCompositeElement {
    fun isVariant(variants:List<String>, strict:Boolean) : Boolean
    val reference:CaosScriptCommandTokenReference
    val commandString:String
}

val CaosScriptIsCommandToken.variant get() = containingCaosFile.variant