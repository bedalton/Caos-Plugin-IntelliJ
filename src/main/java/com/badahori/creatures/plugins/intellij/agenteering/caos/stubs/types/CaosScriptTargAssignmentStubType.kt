package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCTargImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTargAssignmentStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptTargAssignmentStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptTargAssignmentStubType(debugName: String) :
    com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptTargAssignmentStub, CaosScriptCTargImpl>(
        debugName
    ) {

    override fun createPsi(stub: CaosScriptTargAssignmentStub): CaosScriptCTargImpl {
        return CaosScriptCTargImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptTargAssignmentStub, stream: StubOutputStream) {
        stream.writeScope(stub.scope)
        stream.writeNullableList(stub.rvalue) {
            stream.writeExpressionValueType(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptTargAssignmentStub {
        val scope = stream.readScope()
        val caos = stream.readNullableList {
            readExpressionValueType()
        }
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