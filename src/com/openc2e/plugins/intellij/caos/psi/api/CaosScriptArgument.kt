package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiElement

interface CaosScriptArgument : CaosScriptCompositeElement {
    val index:Int
}