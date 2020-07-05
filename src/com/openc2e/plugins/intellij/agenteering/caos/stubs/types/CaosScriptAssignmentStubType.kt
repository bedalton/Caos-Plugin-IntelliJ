package com.openc2e.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.deducer.CaosOp
import com.openc2e.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCAssignmentImpl
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiImplUtil
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.CaosScriptAssignmentStub
import com.openc2e.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptAssignmentStubImpl
import com.openc2e.plugins.intellij.agenteering.caos.utils.readNameAsString

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
        stream.writeName(stub.commandString)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptAssignmentStub {
        val fileName = stream.readNameAsString() ?: UNDEF
        val operation = CaosOp.fromValue(stream.readInt())
        val lvalue = stream.readCaosVarSafe()
        val rvalue = stream.readCaosVarSafe()
        val commandString = stream.readNameAsString() ?: UNDEF
        return CaosScriptAssignmentStubImpl (
                parent = parent,
                fileName = fileName,
                operation = operation,
                lvalue = lvalue,
                rvalue = rvalue,
                enclosingScope = stream.readScope(),
                commandString = commandString
        )
    }

    override fun createStub(element: CaosScriptCAssignmentImpl, parent: StubElement<*>): CaosScriptAssignmentStub {
        return CaosScriptAssignmentStubImpl(
                parent = parent,
                fileName = element.containingFile.name,
                operation = element.op,
                lvalue = element.lvalue?.toCaosVar(),
                rvalue = element.expectsDecimal?.rvalue?.toCaosVar(),
                enclosingScope = CaosScriptPsiImplUtil.getScope(element),
                commandString = element.commandString
        )
    }

    override fun indexStub(stub: CaosScriptAssignmentStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexVarAssignment(stub, indexSink)
    }

}
