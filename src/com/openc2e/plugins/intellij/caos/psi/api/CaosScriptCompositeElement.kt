package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile

interface CaosScriptCompositeElement : PsiElement {
    fun <PsiT: PsiElement> getChildOfType(childType:Class<PsiT>):PsiT? = PsiTreeUtil.getChildOfType(this, childType)
    fun <PsiT: PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT> = PsiTreeUtil.getChildrenOfTypeAsList(this, childType)
    fun <PsiT: PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT? = PsiTreeUtil.getParentOfType(this, parentClass)
    val containingCaosFile:CaosScriptFile
}