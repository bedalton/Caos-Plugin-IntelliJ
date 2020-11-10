package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedGameVarImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptNamedGameVarStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptNamedGameVarStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptNamedGameVarStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptNamedGameVarStub, CaosScriptNamedGameVarImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedGameVarStub): CaosScriptNamedGameVarImpl {
        return CaosScriptNamedGameVarImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedGameVarStub, stream: StubOutputStream) {
        stream.writeInt(stub.type.value)
        stream.writeName(stub.key)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedGameVarStub {
        val type = CaosScriptNamedGameVarType.fromValue(stream.readInt())
        val name = stream.readNameAsString() ?: UNDEF
        return CaosScriptNamedGameVarStubImpl(
                parent = parent,
                type = type,
                key = name
        )
    }

    override fun createStub(element: CaosScriptNamedGameVarImpl, parent: StubElement<*>?): CaosScriptNamedGameVarStub {
        return CaosScriptNamedGameVarStubImpl(
                parent = parent,
                type = element.varType,
                key = element.name ?: UNDEF
        )
    }

}