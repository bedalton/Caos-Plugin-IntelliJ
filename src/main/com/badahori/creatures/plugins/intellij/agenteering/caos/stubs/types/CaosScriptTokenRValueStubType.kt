package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptTokenRvalueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTokenRValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptTokenRValueStubImpl

class CaosScriptTokenRValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptTokenRValueStub, CaosScriptTokenRvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptTokenRValueStub): CaosScriptTokenRvalueImpl {
        return CaosScriptTokenRvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptTokenRValueStub, stream: StubOutputStream) {
        stream.writeCaosVar(stub.caosVar)
        stream.writeList(stub.argumentValues) {
            writeCaosVar(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptTokenRValueStub {
        val selfAsCaosVar = stream.readCaosVar()
        val arguments = stream.readList {
            readCaosVar()
        }
        return CaosScriptTokenRValueStubImpl(parent, selfAsCaosVar, arguments)
    }

    override fun createStub(element: CaosScriptTokenRvalueImpl, parent: StubElement<*>?): CaosScriptTokenRValueStub {
        return CaosScriptTokenRValueStubImpl(parent, element.toCaosVar(), element.argumentValues)
    }

}