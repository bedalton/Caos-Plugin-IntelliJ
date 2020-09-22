package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.variants
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.intellij.psi.PsiFile

open class CaosScriptCompositeElementImpl(node:ASTNode) : ASTWrapperPsiElement(node), CaosScriptCompositeElement {

    override fun <PsiT: PsiElement> getChildOfType(childType:Class<PsiT>):PsiT? = PsiTreeUtil.getChildOfType(this, childType)
    override fun <PsiT: PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT> = PsiTreeUtil.getChildrenOfTypeAsList(this, childType)
    override fun <PsiT: PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT? = PsiTreeUtil.getParentOfType(this, parentClass)
}

val CaosScriptCompositeElement.containingCaosFile : CaosScriptFile? get() = containingFile as? CaosScriptFile

val PsiElement.variant: CaosVariant?
    get() = (containingFile as? CaosScriptFile)?.variant
            ?: (originalElement.containingFile as? CaosScriptFile)?.variant
            ?: (containingFile as? CaosDefFile)?.variants?.firstOrNull()
            ?: (originalElement?.containingFile as? CaosDefFile)?.variants?.firstOrNull()
