package com.openc2e.plugins.intellij.agenteering.caos.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement

open class CaosScriptCompositeElementImpl(node:ASTNode) : ASTWrapperPsiElement(node), CaosScriptCompositeElement {

    override fun <PsiT: PsiElement> getChildOfType(childType:Class<PsiT>):PsiT? = PsiTreeUtil.getChildOfType(this, childType)
    override fun <PsiT: PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT> = PsiTreeUtil.getChildrenOfTypeAsList(this, childType)
    override fun <PsiT: PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT? = PsiTreeUtil.getParentOfType(this, parentClass)
}

val CaosScriptCompositeElement.containingCaosFile : CaosScriptFile? get() = containingFile as? CaosScriptFile