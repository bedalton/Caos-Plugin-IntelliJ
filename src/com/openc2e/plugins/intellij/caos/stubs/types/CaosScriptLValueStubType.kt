package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptLvalueImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptLValueStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptLValueStubImpl

class CaosScriptLValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptLValueStub, CaosScriptLvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptLValueStub): CaosScriptLvalueImpl {
        return CaosScriptLvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptLValueStub, stream: StubOutputStream) {
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptLValueStub {
        return CaosScriptLValueStubImpl(parent, stream.readCaosVar())
    }

    override fun createStub(element: CaosScriptLvalueImpl, parent: StubElement<*>?): CaosScriptLValueStub {
        return CaosScriptLValueStubImpl(parent, element.toCaosVar())
    }

}