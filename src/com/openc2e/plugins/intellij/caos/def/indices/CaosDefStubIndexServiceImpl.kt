package com.openc2e.plugins.intellij.caos.def.indices

import brightscript.intellij.utils.isNotNullOrBlank
import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.def.stubs.interfaces.CaosDefCommandElementStub

class CaosDefStubIndexServiceImpl : CaosDefStubIndexService {


    override fun indexCommand(stub: CaosDefCommandElementStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefCommandElementsByNameIndex.KEY, stub.name)
        if (stub.namespace.isNotNullOrBlank())
            indexSink.occurrence(CaosDefCommandElementsByNamespaceIndex.KEY, stub.namespace!!)
    }


}