package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLiteral
import com.intellij.psi.stubs.StubIndexKey

class CaosScriptExpressionIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosScriptLiteral>(CaosScriptLiteral::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptLiteral> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    companion object {
        val KEY: StubIndexKey<String, CaosScriptLiteral> = IndexKeyUtil.create(CaosScriptExpressionIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptExpressionIndex
    }

}