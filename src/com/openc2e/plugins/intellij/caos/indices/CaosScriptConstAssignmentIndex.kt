package com.openc2e.plugins.intellij.caos.indices

import com.intellij.psi.stubs.StubIndexKey
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptConstantAssignment

class CaosScriptConstAssignmentIndex : CaosStringIndexBase<CaosScriptConstantAssignment>(CaosScriptConstantAssignment::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptConstantAssignment> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptConstantAssignment> = IndexKeyUtil.create(CaosScriptConstAssignmentIndex::class.java)
        const val VERSION = 0;
        val instance = CaosScriptConstAssignmentIndex()
    }

}