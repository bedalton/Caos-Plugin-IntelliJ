package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.deducer.CaosOp
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCAssignmentImpl
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiImplUtil
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptAssignmentStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptAssignmentStubImpl

class CaosScriptAssignmentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptAssignmentStub, CaosScriptCAssignmentImpl>(debugName) {

    override fun createPsi(stub: CaosScriptAssignmentStub): CaosScriptCAssignmentImpl {
        return CaosScriptCAssignmentImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptAssignmentStub, stream: StubOutputStream) {
        stream.writeInt(stub.operation.value)
        stream.writeBoolean(stub.lvalue != null)
        stub.lvalue?.let { stream.writeCaosVar(it) }
        stream.writeBoolean(stub.rvalue != null)
        stub.rvalue?.let { stream.writeCaosVar(it) }
        stream.writeScope(stub.enclosingScope)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptAssignmentStub {
        val operation = CaosOp.fromValue(stream.readInt())
        val lvalue = if (stream.readBoolean()) stream.readCaosVar() else null
        val rvalue = if (stream.readBoolean()) stream.readCaosVar() else null
        return CaosScriptAssignmentStubImpl (
                parent = parent,
                operation = operation,
                lvalue = lvalue,
                rvalue = rvalue,
                enclosingScope = stream.readScope()
        )
    }

    override fun createStub(element: CaosScriptCAssignmentImpl, parent: StubElement<*>): CaosScriptAssignmentStub {
        return CaosScriptAssignmentStubImpl(
                parent = parent,
                operation = element.op,
                lvalue = element.lvalue?.toCaosVar(),
                rvalue = element.expectsDecimal?.rvalue?.toCaosVar(),
                enclosingScope = CaosScriptPsiImplUtil.getScope(element)
        )
    }

}
