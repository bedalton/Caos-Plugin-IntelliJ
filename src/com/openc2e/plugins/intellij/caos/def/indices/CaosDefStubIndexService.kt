package com.openc2e.plugins.intellij.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.def.stubs.interfaces.CaosDefCommandElementStub

interface CaosDefStubIndexService {

    fun indexCommand(stub:CaosDefCommandElementStub, indexSink: IndexSink);
}