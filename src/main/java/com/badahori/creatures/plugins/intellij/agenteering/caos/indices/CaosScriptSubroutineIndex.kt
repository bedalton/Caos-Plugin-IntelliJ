package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine

class CaosScriptSubroutineIndex : CaosStringIndexBase<CaosScriptSubroutine>(CaosScriptSubroutine::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptSubroutine> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptSubroutine> = IndexKeyUtil.create(CaosScriptSubroutineIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptSubroutineIndex()
    }

}