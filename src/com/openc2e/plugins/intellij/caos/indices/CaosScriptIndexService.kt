package com.openc2e.plugins.intellij.caos.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptSubroutineStub

interface CaosScriptIndexService {
    fun indexSubroutine(subroutineStub: CaosScriptSubroutineStub, indexSink: IndexSink)
}