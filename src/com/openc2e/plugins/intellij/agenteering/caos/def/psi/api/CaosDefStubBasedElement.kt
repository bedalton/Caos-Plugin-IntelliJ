package com.openc2e.plugins.intellij.agenteering.caos.def.psi.api

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement

interface CaosDefStubBasedElement<StubT:StubElement<*>> : StubBasedPsiElement<StubT>, CaosDefCompositeElement {

}