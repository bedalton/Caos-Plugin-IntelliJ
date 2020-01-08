package com.openc2e.plugins.intellij.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefTypeDefinitionStub

interface CaosDefStubIndexService {

    fun indexCommand(stub:CaosDefCommandDefinitionStub, indexSink: IndexSink)
    fun indexTypeDef(stub:CaosDefTypeDefinitionStub, indexSink: IndexSink)
}