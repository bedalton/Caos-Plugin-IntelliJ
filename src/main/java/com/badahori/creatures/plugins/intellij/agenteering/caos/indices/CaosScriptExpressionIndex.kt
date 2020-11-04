package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.intellij.psi.stubs.StubIndexKey

class CaosScriptExpressionIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosScriptRvalue>(CaosScriptRvalue::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptRvalue> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    companion object {
        val KEY: StubIndexKey<String, CaosScriptRvalue> = IndexKeyUtil.create(CaosScriptExpressionIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptExpressionIndex
    }

}