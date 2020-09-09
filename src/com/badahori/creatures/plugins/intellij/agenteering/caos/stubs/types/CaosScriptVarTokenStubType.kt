package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptVarTokenImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptVarTokenStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptVarTokenStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptVarTokenStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptVarTokenStub, CaosScriptVarTokenImpl>(debugName) {

    override fun createPsi(stub: CaosScriptVarTokenStub): CaosScriptVarTokenImpl {
        return CaosScriptVarTokenImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptVarTokenStub, stream: StubOutputStream) {
        stream.writeName(stub.varGroup.value)
        stream.writeInt(stub.varIndex ?: -1)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptVarTokenStub {
        val typeString = stream.readNameAsString().nullIfEmpty()
        val group = if (typeString != null)
            CaosScriptVarTokenGroup.fromValue(typeString)
        else
            CaosScriptVarTokenGroup.UNKNOWN
        val varIndex = stream.readInt().let {
            if (it >= 0)
                it
            else
                null
        }
        return CaosScriptVarTokenStubImpl(
                parent = parent,
                varGroup = group,
                varIndex = varIndex
        )
    }

    override fun createStub(element: CaosScriptVarTokenImpl, parent: StubElement<*>): CaosScriptVarTokenStub {
        return CaosScriptVarTokenStubImpl(
                parent = parent,
                varGroup = element.varGroup,
                varIndex = element.varIndex
        )
    }

}
