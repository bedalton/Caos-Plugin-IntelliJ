package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement

abstract class CaosScriptStubElementType<StubT : StubElement<PsiT>?, PsiT : CaosScriptCompositeElement?>(
    debugName: String
) : IStubElementType<StubT, PsiT>(debugName, CaosScriptLanguage) {
    /*@NotNull
    protected CaosScriptStubIndexService getService() {
        return ServiceManager.getService(CaosScriptStubIndexService.class);
    }*/
    override fun getExternalId(): String {
        return "caos." + super.toString()
    }

    override fun indexStub(stub: StubT & Any, indexSink: IndexSink) {
        // ignore
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        val element = node?.psi ?: return false
        var file = element.containingFile
        if (file == null) file = element.originalElement.containingFile
        return if (file == null) false else file.virtualFile !is CaosVirtualFile
    }
}