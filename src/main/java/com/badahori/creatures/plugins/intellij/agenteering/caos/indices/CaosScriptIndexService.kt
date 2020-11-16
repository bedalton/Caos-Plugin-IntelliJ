package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.IndexSink
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.*

interface CaosScriptIndexService {
    fun indexSubroutine(subroutineStub: CaosScriptSubroutineStub, indexSink: IndexSink)
    fun indexVarAssignment(stub: CaosScriptAssignmentStub, indexSink: IndexSink)
    fun indexEventScript(stub: CaosScriptEventScriptStub, indexSink: IndexSink)
    fun indexNamedGameVar(stub: CaosScriptNamedGameVarStub, indexSink: IndexSink)
}