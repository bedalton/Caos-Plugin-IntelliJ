package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCAssignmentImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCTargImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptTargAssignmentStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptTargAssignmentStubImpl

class CaosScriptTargAssignmentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptTargAssignmentStub, CaosScriptCTargImpl>(debugName) {

    override fun createPsi(stub: CaosScriptTargAssignmentStub): CaosScriptCTargImpl {
        return CaosScriptCTargImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptTargAssignmentStub, stream: StubOutputStream) {
        stream.writeScope(stub.scope)
        stream.writeBoolean(stub.rvalue != null)
        if (stub.rvalue != null)
            stream.writeCaosVar(stub.rvalue!!)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptTargAssignmentStub {
        val scope = stream.readScope()
        val caos = if (stream.readBoolean())
            stream.readCaosVar();
        else
            null
        return CaosScriptTargAssignmentStubImpl(parent, scope, caos)
    }

    override fun createStub(element: CaosScriptCTargImpl, parent: StubElement<*>?): CaosScriptTargAssignmentStub {
        return CaosScriptTargAssignmentStubImpl(
                parent = parent,
                scope = element.scope,
                rvalue = element.rVar
        )
    }
}