package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiNamedElement
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference

interface CaosScriptIsCommandToken : PsiNamedElement {
    fun isVariant(variants:List<String>, strict:Boolean) : Boolean
    val reference:CaosScriptCommandTokenReference
}