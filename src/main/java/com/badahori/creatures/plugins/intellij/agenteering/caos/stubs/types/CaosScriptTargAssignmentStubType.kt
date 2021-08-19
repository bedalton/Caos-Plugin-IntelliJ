package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCTargImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTargAssignmentStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptTargAssignmentStubImpl

class CaosScriptTargAssignmentStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptTargAssignmentStub, CaosScriptCTargImpl>(debugName) {

    override fun createPsi(stub: CaosScriptTargAssignmentStub): CaosScriptCTargImpl {
        return CaosScriptCTargImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptTargAssignmentStub, stream: StubOutputStream) {
        stream.writeScope(stub.scope)
        val rvalue = stub.rvalue
        stream.writeBoolean(rvalue != null)
        if (rvalue != null) {
            stream.writeList(rvalue) {
                stream.writeExpressionValueType(it)
            }
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptTargAssignmentStub {
        val scope = stream.readScope()
        val caos = if (stream.readBoolean())
            stream.readList {
                stream.readExpressionValueType()
            }
        else
            null
        return CaosScriptTargAssignmentStubImpl(parent, scope, caos)
    }

    override fun createStub(element: CaosScriptCTargImpl, parent: StubElement<*>?): CaosScriptTargAssignmentStub {
        return CaosScriptTargAssignmentStubImpl(
                parent = parent,
                scope = element.scope,
                rvalue = element.rvalue?.inferredType
        )
    }
}