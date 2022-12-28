package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueItemType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.stubs.StubElement

interface CatalogueCompositeElement : PsiElement

interface CatalogueEntryElement<PsiT: CatalogueCompositeElement, StubT: StubElement<PsiT>>: CatalogueStubBasedElement<StubT>, PsiNameIdentifierOwner, CatalogueCompositeElement {
    val itemCount: Int
    val type: CatalogueItemType
    val itemsAsStrings: List<String>

    val isArray get() = type == CatalogueItemType.ARRAY
    val isTag get() = type == CatalogueItemType.TAG
}