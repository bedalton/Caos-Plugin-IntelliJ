package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

class CaosScriptStringLiteralIndex : CaosStringIndexBase<CaosScriptStringLike>(CaosScriptStringLike::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptStringLike> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION


    companion object {
        val KEY: StubIndexKey<String, CaosScriptStringLike> = IndexKeyUtil.create(CaosScriptStringLiteralIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptStringLiteralIndex()
    }

}