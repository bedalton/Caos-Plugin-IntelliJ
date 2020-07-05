package com.openc2e.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.StubIndexKey
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedVar

class CaosScriptNamedVarUseIndex : CaosStringIndexBase<CaosScriptNamedVar>(CaosScriptNamedVar::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptNamedVar> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptNamedVar> = IndexKeyUtil.create(CaosScriptNamedVarUseIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptNamedVarUseIndex()
    }

}