package com.openc2e.plugins.intellij.caos.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.stubs.api.*

class CaosScriptIndexServiceImpl : CaosScriptIndexService {

    override fun indexSubroutine(subroutineStub: CaosScriptSubroutineStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptSubroutineIndex.KEY, subroutineStub.name)
    }

    override fun indexNamedVar(stub: CaosScriptNamedVarStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptNamedVarUseIndex.KEY, stub.name)
    }

    override fun indexConstantAssignment(stub: CaosScriptConstantAssignmentStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptConstAssignmentIndex.KEY, stub.name)
    }

    override fun indexNamedConstant(stub: CaosScriptNamedConstantStub, indexSink: IndexSink) {
        // Is this necessary
    }

    override fun indexVarAssignment(stub: CaosScriptAssignmentStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptVarAssignmentIndex.KEY, stub.fileName)
    }

}