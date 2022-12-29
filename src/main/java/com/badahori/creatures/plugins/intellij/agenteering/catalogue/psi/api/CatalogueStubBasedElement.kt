package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement

interface CatalogueStubBasedElement<StubT:StubElement<*>> : StubBasedPsiElement<StubT>, CatalogueCompositeElement