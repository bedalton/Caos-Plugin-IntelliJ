package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

open class CaosDefCompositeElementImpl(node:ASTNode) : ASTWrapperPsiElement(node), CaosDefCompositeElement {
    override val tokenType: IElementType
        get() = node.elementType
}

val CaosDefCompositeElement.containingCaosDefFile : CaosDefFile get() = containingFile as CaosDefFile