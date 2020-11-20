package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptEventScriptImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptEventScriptStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptEventScriptStubImpl
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink

class CaosScriptEventScriptStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptEventScriptStub, CaosScriptEventScriptImpl>(debugName) {
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

    override fun indexStub(stub: CaosScriptEventScriptStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexEventScript(stub, indexSink)
    }

}