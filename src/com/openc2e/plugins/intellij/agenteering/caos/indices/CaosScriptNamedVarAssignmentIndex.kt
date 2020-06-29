package com.openc2e.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.StubIndexKey
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedVarAssignment

class CaosScriptNamedVarAssignmentIndex : CaosStringIndexBase<CaosScriptNamedVarAssignment>(CaosScriptNamedVarAssignment::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptNamedVarAssignment> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptNamedVarAssignment> = IndexKeyUtil.create(CaosScriptNamedVarAssignmentIndex::class.java)
        const val VERSION = 0;
        val instance = CaosScriptNamedVarAssignmentIndex()
    }

}