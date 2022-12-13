package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueLanguage

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueStubBasedElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement

open class CatalogueStubBasedElementImpl<StubT : StubElement<out PsiElement>>: StubBasedPsiElementBase<StubT>, CatalogueStubBasedElement<StubT> {

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)
    override fun getLanguage(): Language = CatalogueLanguage
    override fun toString(): String  = elementType.toString()
}