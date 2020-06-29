package com.openc2e.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.CaosScriptSubroutineImpl
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.CaosScriptSubroutineStub
import com.openc2e.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptSubroutineStubImpl
import com.openc2e.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptSubroutineStubType(debugName:String) : CaosScriptStubElementType<CaosScriptSubroutineStub, CaosScriptSubroutineImpl>(debugName) {

    override fun createPsi(stub: CaosScriptSubroutineStub): CaosScriptSubroutineImpl {
        return CaosScriptSubroutineImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptSubroutineStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptSubroutineStub {
        val name = stream.readNameAsString()
                ?: ""
        return CaosScriptSubroutineStubImpl(parent, name)
    }

    override fun createStub(element: CaosScriptSubroutineImpl, parent: StubElement<*>): CaosScriptSubroutineStub {
        return CaosScriptSubroutineStubImpl(
                parent = parent,
                name = element.name ?: ""
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as? CaosScriptSubroutine)?.name.nullIfEmpty() != null
    }

    override fun indexStub(stub: CaosScriptSubroutineStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexSubroutine(stub, indexSink)
    }

}
