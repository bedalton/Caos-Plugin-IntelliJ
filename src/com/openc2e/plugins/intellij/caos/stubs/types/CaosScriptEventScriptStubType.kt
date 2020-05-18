package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptEventScriptImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptEventScriptStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptEventScriptStubImpl

class CaosScriptEventScriptStubType(debugName:String) : CaosScriptStubElementType<CaosScriptEventScriptStub, CaosScriptEventScriptImpl>(debugName) {
    override fun createPsi(parent: CaosScriptEventScriptStub): CaosScriptEventScriptImpl {
        return CaosScriptEventScriptImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptEventScriptStub, stream: StubOutputStream) {
        stream.writeInt(stub.family)
        stream.writeInt(stub.genus)
        stream.writeInt(stub.species)
        stream.writeInt(stub.eventNumber)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptEventScriptStub {
        return CaosScriptEventScriptStubImpl(
                parent = parent,
                family = stream.readInt(),
                genus = stream.readInt(),
                species = stream.readInt(),
                eventNumber = stream.readInt()
        )
    }

    override fun createStub(element: CaosScriptEventScriptImpl, parent: StubElement<*>?): CaosScriptEventScriptStub {
        return CaosScriptEventScriptStubImpl(
                parent = parent,
                family = element.family,
                genus = element.genus,
                species = element.species,
                eventNumber = element.eventNumber
        )
    }

}