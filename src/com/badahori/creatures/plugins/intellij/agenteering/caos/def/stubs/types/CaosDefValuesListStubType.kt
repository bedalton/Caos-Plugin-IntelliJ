package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefStubIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefValuesListElementImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefValuesListStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*

class CaosDefValuesListStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubElementType<CaosDefValuesListStub, CaosDefValuesListElementImpl>(debugName) {

    override fun createPsi(stub: CaosDefValuesListStub): CaosDefValuesListElementImpl {
        return CaosDefValuesListElementImpl(stub, this)
    }

    override fun serialize(stub: CaosDefValuesListStub, stream: StubOutputStream) {
        stream.writeName(stub.typeName)
        stream.writeList(stub.keys, StubOutputStream::writeValuesListValue)
        stream.writeName(stub.typeNote.orEmpty())
        stream.writeBoolean(stub.isBitflags)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefValuesListStub {
        val name = stream.readNameAsString().nullIfEmpty() ?: CaosDefPsiImplUtil.UnknownReturn
        val keys = stream.readList(StubInputStream::readValuesListValue).filterNotNull()
        val typeNote = stream.readNameAsString()?.nullIfEmpty()
        val isBitflags = stream.readBoolean()
        return CaosDefValuesListStubImpl (
                parent = parent,
                typeName = name,
                keys = keys,
                typeNote = typeNote,
                isBitflags = isBitflags
        )
    }

    override fun createStub(element: CaosDefValuesListElementImpl, parent: StubElement<*>): CaosDefValuesListStub {
        return CaosDefValuesListStubImpl (
                parent = parent,
                typeName = element.typeName,
                keys = element.valuesListValues,
                typeNote = element.typeNoteString,
                isBitflags = element.isBitflags
        )
    }

    override fun indexStub(stub: CaosDefValuesListStub, indexSink: IndexSink) {
       ServiceManager.getService(CaosDefStubIndexService::class.java).indexValuesList(stub, indexSink)
    }
}