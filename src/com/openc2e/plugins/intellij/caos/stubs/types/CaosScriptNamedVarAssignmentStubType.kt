package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptNamedVarAssignmentImpl
import com.openc2e.plugins.intellij.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptNamedVarAssignmentStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptNamedVarAssignmentStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptNamedVarAssignmentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptNamedVarAssignmentStub, CaosScriptNamedVarAssignmentImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedVarAssignmentStub): CaosScriptNamedVarAssignmentImpl {
        return CaosScriptNamedVarAssignmentImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedVarAssignmentStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeCaosVarSafe(stub.value)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedVarAssignmentStub {
        val name = stream.readNameAsString() ?: UNDEF
        val value = stream.readCaosVarSafe()
        return CaosScriptNamedVarAssignmentStubImpl(
                parent = parent,
                name = name,
                value = value
        )
    }

    override fun createStub(element: CaosScriptNamedVarAssignmentImpl, parent: StubElement<*>?): CaosScriptNamedVarAssignmentStub {
        return CaosScriptNamedVarAssignmentStubImpl(
                parent = parent,
                name = element.name,
                value = element.value
        )
    }

    override fun indexStub(stub: CaosScriptNamedVarAssignmentStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexNamedVarAssignment(stub, indexSink)
    }

}