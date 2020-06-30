package com.openc2e.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.StubIndexKey
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment

class CaosScriptVarAssignmentIndex : CaosStringIndexBase<CaosScriptCAssignment>(CaosScriptCAssignment::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptCAssignment> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptCAssignment> = IndexKeyUtil.create(CaosScriptVarAssignmentIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptVarAssignmentIndex()
    }

}