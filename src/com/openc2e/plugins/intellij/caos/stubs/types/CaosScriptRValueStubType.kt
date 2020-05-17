package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptRvalueImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptRValueStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptRValueStubImpl

class CaosScriptRValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptRValueStub, CaosScriptRvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRValueStub): CaosScriptRvalueImpl {
        return CaosScriptRvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRValueStub, stream: StubOutputStream) {
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRValueStub {
        return CaosScriptRValueStubImpl(parent, stream.readCaosVar())
    }

    override fun createStub(element: CaosScriptRvalueImpl, parent: StubElement<*>?): CaosScriptRValueStub {
        return CaosScriptRValueStubImpl(parent, element.toCaosVar())
    }

}