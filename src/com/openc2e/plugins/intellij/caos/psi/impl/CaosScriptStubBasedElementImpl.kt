package com.openc2e.plugins.intellij.caos.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefStubBasedElement

class CaosScriptStubBasedElementImpl<StubT : StubElement<out PsiElement>>: StubBasedPsiElementBase<StubT>, CaosDefStubBasedElement<StubT> {

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)
    override fun getLanguage(): Language = CaosDefLanguage.instance
    override fun toString(): String  = elementType.toString()
    override fun <PsiT: PsiElement> getChildOfType(childType:Class<PsiT>):PsiT? = PsiTreeUtil.getChildOfType(this, childType)
    override fun <PsiT: PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT> = PsiTreeUtil.getChildrenOfTypeAsList(this, childType)
    override fun <PsiT: PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT? = PsiTreeUtil.getParentOfType(this, parentClass)
}