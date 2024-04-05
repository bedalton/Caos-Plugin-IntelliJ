package com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueArrayStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueTagStub
import com.intellij.psi.stubs.IndexSink

object CatalogueStubIndexService {

    fun indexTag(stub: CatalogueTagStub, indexSink: IndexSink) {
        indexSink.occurrence(CatalogueEntryElementIndex.KEY, stub.name)
    }

    fun indexArray(stub: CatalogueArrayStub, indexSink: IndexSink) {
        indexSink.occurrence(CatalogueEntryElementIndex.KEY, stub.name)
    }
}