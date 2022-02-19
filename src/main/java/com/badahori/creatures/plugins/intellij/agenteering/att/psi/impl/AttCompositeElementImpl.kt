package com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosPresentation
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

open class AttCompositeElementImpl(node:ASTNode) : ASTWrapperPsiElement(node), CaosScriptCompositeElement {

    override fun <PsiT: PsiElement> getChildOfType(childType:Class<PsiT>):PsiT? = PsiTreeUtil.getChildOfType(this, childType)
    override fun <PsiT: PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT> = PsiTreeUtil.getChildrenOfTypeAsList(this, childType)
    override fun <PsiT: PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT? = PsiTreeUtil.getParentOfType(this, parentClass)
    override val descriptiveText:String? get() = null
    override val locationString:String? get() = null
    override val icon: Icon? get() = null
    override fun getPresentation():ItemPresentation = CaosPresentation(descriptiveText ?: node.text, locationString, icon)

    override val tokenType: IElementType
        get() = node.elementType
}
