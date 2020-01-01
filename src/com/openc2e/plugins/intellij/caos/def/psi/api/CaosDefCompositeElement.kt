package com.openc2e.plugins.intellij.caos.def.psi.api

import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile

interface CaosDefCompositeElement : PsiElement {
    fun <PsiT:PsiElement> getChildOfType(childType:Class<PsiT>):PsiT?
    fun <PsiT:PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT>
    fun <PsiT:PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT?
    val containingCaosDefFile:CaosDefFile
}