package com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueArrayStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueTagStub
import com.intellij.psi.stubs.IndexSink

class CatalogueStubIndexServiceImpl : CatalogueStubIndexService {

    override fun indexTag(stub: CatalogueTagStub, indexSink: IndexSink) {
        indexSink.occurrence(CatalogueEntryElementIndex.KEY, stub.name)
    }

    override fun indexArray(stub: CatalogueArrayStub, indexSink: IndexSink) {
        indexSink.occurrence(CatalogueEntryElementIndex.KEY, stub.name)
    }

}