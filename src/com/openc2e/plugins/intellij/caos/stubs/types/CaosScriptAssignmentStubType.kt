package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.deducer.CaosOp
import com.openc2e.plugins.intellij.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCAssignmentImpl
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiImplUtil
import com.openc2e.plugins.intellij.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptAssignmentStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptAssignmentStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptAssignmentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptAssignmentStub, CaosScriptCAssignmentImpl>(debugName) {

    override fun createPsi(stub: CaosScriptAssignmentStub): CaosScriptCAssignmentImpl {
        return CaosScriptCAssignmentImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptAssignmentStub, stream: StubOutputStream) {
        stream.writeName(stub.fileName)
        stream.writeInt(stub.operation.value)
        stream.writeCaosVarSafe(stub.lvalue)
        stream.writeCaosVarSafe(stub.rvalue)
        stream.writeScope(stub.enclosingScope)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptAssignmentStub {
        val fileName = stream.readNameAsString() ?: UNDEF
        val operation = CaosOp.fromValue(stream.readInt())
        val lvalue = stream.readCaosVarSafe()
        val rvalue = stream.readCaosVarSafe()
        return CaosScriptAssignmentStubImpl (
                parent = parent,
                fileName = fileName,
                operation = operation,
                lvalue = lvalue,
                rvalue = rvalue,
                enclosingScope = stream.readScope()
        )
    }

    override fun createStub(element: CaosScriptCAssignmentImpl, parent: StubElement<*>): CaosScriptAssignmentStub {
        return CaosScriptAssignmentStubImpl(
                parent = parent,
                fileName = element.containingFile.name,
                operation = element.op,
                lvalue = element.lvalue?.toCaosVar(),
                rvalue = element.expectsDecimal?.rvalue?.toCaosVar(),
                enclosingScope = CaosScriptPsiImplUtil.getScope(element)
        )
    }

    override fun indexStub(stub: CaosScriptAssignmentStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexVarAssignment(stub, indexSink)
    }

}
