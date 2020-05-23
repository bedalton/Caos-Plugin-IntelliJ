package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.deducer.CaosNumber
import com.openc2e.plugins.intellij.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptConstantAssignmentImpl
import com.openc2e.plugins.intellij.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptConstantAssignmentStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptConstantAssignmentStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptConstantAssignmentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptConstantAssignmentStub, CaosScriptConstantAssignmentImpl>(debugName) {
    override fun createPsi(parent: CaosScriptConstantAssignmentStub): CaosScriptConstantAssignmentImpl {
        return CaosScriptConstantAssignmentImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptConstantAssignmentStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeCaosNumber(stub.value)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptConstantAssignmentStub {
        val name = stream.readNameAsString() ?: UNDEF
        val value = stream.readCaosNumber()
        return CaosScriptConstantAssignmentStubImpl(
                parent = parent,
                name = name,
                value = value
        )
    }

    override fun createStub(element: CaosScriptConstantAssignmentImpl, parent: StubElement<*>?): CaosScriptConstantAssignmentStub {
        return CaosScriptConstantAssignmentStubImpl(
                parent = parent,
                name = element.name,
                value = element.value
        )
    }

    override fun indexStub(stub: CaosScriptConstantAssignmentStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexConstantAssignment(stub, indexSink)
    }

}