package com.openc2e.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedGameVarImpl
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.CaosScriptNamedGameVarType
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.CaosScriptNamedGameVarStub
import com.openc2e.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptNamedGameVarStubImpl
import com.openc2e.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptNamedGameVarStubType(debugName:String) : CaosScriptStubElementType<CaosScriptNamedGameVarStub, CaosScriptNamedGameVarImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedGameVarStub): CaosScriptNamedGameVarImpl {
        return CaosScriptNamedGameVarImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedGameVarStub, stream: StubOutputStream) {
        stream.writeInt(stub.type.value)
        stream.writeName(stub.name)
        stream.writeCaosVar(stub.key)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedGameVarStub {
        val type = CaosScriptNamedGameVarType.fromValue(stream.readInt())
        val name = stream.readNameAsString() ?: UNDEF
        return CaosScriptNamedGameVarStubImpl(
                parent = parent,
                type = type,
                name = name,
                key = stream.readCaosVar()
        )
    }

    override fun createStub(element: CaosScriptNamedGameVarImpl, parent: StubElement<*>?): CaosScriptNamedGameVarStub {
        return CaosScriptNamedGameVarStubImpl(
                parent = parent,
                type = element.varType,
                name = element.name ?: UNDEF,
                key = element.key
        )
    }

}