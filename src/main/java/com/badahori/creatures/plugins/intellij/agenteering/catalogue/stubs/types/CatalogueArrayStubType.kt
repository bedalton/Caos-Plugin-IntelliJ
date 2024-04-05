package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeStringList
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueStubIndexService
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl.CatalogueArrayImpl
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueArrayStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueItemType
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.impl.CatalogueArrayStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CatalogueArrayStubType(debugName:String) : CatalogueStubElementType<CatalogueArrayStub, CatalogueArrayImpl>(debugName) {

    override fun createPsi(stub: CatalogueArrayStub): CatalogueArrayImpl {
        return CatalogueArrayImpl(stub, this)
    }

    override fun serialize(stub: CatalogueArrayStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeInt(stub.type.value)
        stream.writeInt(stub.itemCount)
        stream.writeBoolean(stub.override)
        stream.writeInt(stub.expectedValuesCount)
        stream.writeStringList(stub.items)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CatalogueArrayStub {

        return CatalogueArrayStubImpl(
            parent,
            name = stream.readNameString() ?: "<<UNDEF>>",
            type = CatalogueItemType.fromValue(stream.readInt()),
            itemCount = stream.readInt(),
            override = stream.readBoolean(),
            expectedValuesCount = stream.readInt(),
            items = stream.readStringList()
        )
    }

    override fun createStub(element: CatalogueArrayImpl, parent: StubElement<*>): CatalogueArrayStub {
        return CatalogueArrayStubImpl(
            parent,
            name = element.name ?: UNDEF,
            type = element.type,
            override = element.override != null,
            itemCount = element.itemCount,
            expectedValuesCount = element.expectedValueCount,
            items = element.itemsAsStrings
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as? CatalogueArray)?.name?.isNotNullOrBlank() == true
    }

    override fun indexStub(stub: CatalogueArrayStub, indexSink: IndexSink) {
        CatalogueStubIndexService.indexArray(stub, indexSink)
    }
}