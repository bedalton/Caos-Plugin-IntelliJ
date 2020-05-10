package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.deducer.CaosScope

interface CaosScriptArgument : CaosScriptCompositeElement {
    val index:Int
    val scopes:List<CaosScope>
}