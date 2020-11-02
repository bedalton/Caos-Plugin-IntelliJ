package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptConstantAssignment

class CaosScriptConstAssignmentIndex : CaosStringIndexBase<CaosScriptConstantAssignment>(CaosScriptConstantAssignment::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptConstantAssignment> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptConstantAssignment> = IndexKeyUtil.create(CaosScriptConstAssignmentIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptConstAssignmentIndex()
    }

}