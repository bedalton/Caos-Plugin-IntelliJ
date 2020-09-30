package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression
import com.intellij.psi.stubs.StubIndexKey

class CaosScriptExpressionIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosScriptExpression>(CaosScriptExpression::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptExpression> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    companion object {
        val KEY: StubIndexKey<String, CaosScriptExpression> = IndexKeyUtil.create(CaosScriptExpressionIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptExpressionIndex
    }

}