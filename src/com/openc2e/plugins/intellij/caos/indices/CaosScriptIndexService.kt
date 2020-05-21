package com.openc2e.plugins.intellij.caos.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.stubs.api.*

interface CaosScriptIndexService {
    fun indexSubroutine(subroutineStub: CaosScriptSubroutineStub, indexSink: IndexSink)
    fun indexNamedVar(stub: CaosScriptNamedVarStub, indexSink: IndexSink)
    fun indexConstantAssignment(stub: CaosScriptConstantAssignmentStub, indexSink: IndexSink)
    fun indexNamedConstant(stub: CaosScriptNamedConstantStub, indexSink: IndexSink)
    fun indexVarAssignment(stub: CaosScriptAssignmentStub, indexSink: IndexSink)
    fun indexNamedVarAssignment(stub: CaosScriptNamedVarAssignmentStub, indexSink: IndexSink)
}