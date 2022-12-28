package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeStringList
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueStubIndexService
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl.CatalogueTagImpl
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueItemType
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueTagStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.impl.CatalogueTagStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.utils.readString
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CatalogueTagStubType(debugName:String) : CatalogueStubElementType<CatalogueTagStub, CatalogueTagImpl>(debugName) {

    override fun createPsi(stub: CatalogueTagStub): CatalogueTagImpl {
        return CatalogueTagImpl(stub, this)
    }

    override fun serialize(stub: CatalogueTagStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeInt(stub.type.value)
        stream.writeInt(stub.itemCount)
        stream.writeStringList(stub.items)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CatalogueTagStub {

        return CatalogueTagStubImpl(
            parent,
            name = stream.readNameString() ?: "<<UNDEF>>",
            type = CatalogueItemType.fromValue(stream.readInt()),
            itemCount = stream.readInt(),
            items = stream.readStringList()
        )
    }

    override fun createStub(element: CatalogueTagImpl, parent: StubElement<*>): CatalogueTagStub {
        return CatalogueTagStubImpl(
            parent,
            name = element.name ?: UNDEF,
            type = element.type,
            itemCount = element.itemCount,
            items = element.itemsAsStrings
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as? CatalogueTag)?.name?.isNotNullOrBlank() == true
    }

    override fun indexStub(stub: CatalogueTagStub, indexSink: IndexSink) {
        ServiceManager.getService(CatalogueStubIndexService::class.java).indexTag(stub, indexSink)
    }
}