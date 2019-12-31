package com.openc2e.plugins.intellij.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefCommandDefinitionStub

interface CaosDefStubIndexService {

    fun indexCommand(stub:CaosDefCommandDefinitionStub, indexSink: IndexSink);
}