package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

interface CaosScriptIsCommandToken : PsiNamedElement {
    val index:Int
    fun isVariant(variants:List<String>, strict:Boolean) : Boolean
}