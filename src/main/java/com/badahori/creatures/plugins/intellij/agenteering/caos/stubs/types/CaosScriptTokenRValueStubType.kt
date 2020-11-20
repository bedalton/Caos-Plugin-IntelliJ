package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptTokenRvalueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTokenRValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptTokenRValueStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptTokenRValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptTokenRValueStub, CaosScriptTokenRvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptTokenRValueStub): CaosScriptTokenRvalueImpl {
        return CaosScriptTokenRvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptTokenRValueStub, stream: StubOutputStream) {
        stream.writeName(stub.tokenText)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptTokenRValueStub {
        val token = stream.readNameAsString()
        return CaosScriptTokenRValueStubImpl(parent, token ?: UNDEF)
    }

    override fun createStub(element: CaosScriptTokenRvalueImpl, parent: StubElement<*>?): CaosScriptTokenRValueStub {
        return CaosScriptTokenRValueStubImpl(parent, element.text)
    }

}