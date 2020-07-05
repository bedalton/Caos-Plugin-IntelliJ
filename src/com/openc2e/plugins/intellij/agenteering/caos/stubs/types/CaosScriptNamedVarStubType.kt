package com.openc2e.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedVarImpl
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.CaosScriptNamedVarStub
import com.openc2e.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptNamedVarStubImpl
import com.openc2e.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptNamedVarStubType(debugName:String) : CaosScriptStubElementType<CaosScriptNamedVarStub, CaosScriptNamedVarImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedVarStub): CaosScriptNamedVarImpl {
        return CaosScriptNamedVarImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedVarStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeScope(stub.scope)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedVarStub {
        val name = stream.readNameAsString() ?: "{UNDEF}"
        return CaosScriptNamedVarStubImpl(
                parent = parent,
                name = name,
                scope = stream.readScope()
        )
    }

    override fun createStub(element: CaosScriptNamedVarImpl, parent: StubElement<*>?): CaosScriptNamedVarStub {
        return CaosScriptNamedVarStubImpl(
                parent = parent,
                name = element.name,
                scope = element.scope
        )
    }

    override fun indexStub(stub: CaosScriptNamedVarStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexNamedVar(stub, indexSink)
    }

}