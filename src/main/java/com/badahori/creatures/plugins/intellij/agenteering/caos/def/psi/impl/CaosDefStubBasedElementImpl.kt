package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefStubBasedElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

open class CaosDefStubBasedElementImpl<StubT : StubElement<out PsiElement>>: StubBasedPsiElementBase<StubT>, CaosDefStubBasedElement<StubT> {

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)
    override fun getLanguage(): Language = CaosDefLanguage.instance
    override fun toString(): String  = elementType.toString()
    override val tokenType: IElementType
        get() = node.elementType
}