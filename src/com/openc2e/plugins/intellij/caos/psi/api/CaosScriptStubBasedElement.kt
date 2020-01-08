package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement

interface CaosScriptStubBasedElement<StubT:StubElement<*>> : StubBasedPsiElement<StubT>, CaosScriptCompositeElement {

}