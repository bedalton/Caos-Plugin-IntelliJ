package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.IndexSink
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.*

class CaosScriptIndexServiceImpl : CaosScriptIndexService {

    override fun indexSubroutine(subroutineStub: CaosScriptSubroutineStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptSubroutineIndex.KEY, subroutineStub.name)
    }

    override fun indexVarAssignment(stub: CaosScriptAssignmentStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptVarAssignmentIndex.KEY, stub.fileName)
    }

    override fun indexEventScript(stub: CaosScriptEventScriptStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosScriptEventScriptIndex.KEY, CaosScriptEventScriptIndex.toIndexKey(stub.family, stub.genus, stub.species, stub.eventNumber))
    }

}