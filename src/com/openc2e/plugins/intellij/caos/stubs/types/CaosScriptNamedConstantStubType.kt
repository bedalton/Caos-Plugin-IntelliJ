package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptNamedConstantImpl
import com.openc2e.plugins.intellij.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptNamedConstantStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptNamedConstantStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptNamedConstantStubType(debugName:String) : CaosScriptStubElementType<CaosScriptNamedConstantStub, CaosScriptNamedConstantImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedConstantStub): CaosScriptNamedConstantImpl {
        return CaosScriptNamedConstantImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedConstantStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeScope(stub.scope)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedConstantStub {
        val name = stream.readNameAsString() ?: UNDEF
        return CaosScriptNamedConstantStubImpl(
                parent = parent,
                name = name,
                scope = stream.readScope()
        )
    }

    override fun createStub(element: CaosScriptNamedConstantImpl, parent: StubElement<*>?): CaosScriptNamedConstantStub {
        return CaosScriptNamedConstantStubImpl(
                parent = parent,
                name = element.name,
                scope = element.scope
        )
    }

    override fun indexStub(stub: CaosScriptNamedConstantStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexNamedConstant(stub, indexSink)
    }

}