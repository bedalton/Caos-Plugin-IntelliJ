package com.openc2e.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.StubIndexKey
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.agenteering.caos.indices.CaosScriptCaseInsensitiveStringIndexBase
import com.openc2e.plugins.intellij.agenteering.caos.indices.IndexKeyUtil

class CaosDefCommandElementsByNameIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosDefCommandDefElement>(CaosDefCommandDefElement::class.java) {

    override fun getKey(): StubIndexKey<String, CaosDefCommandDefElement> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    companion object {
        private const val VERSION = 2
        @JvmStatic
        val KEY = IndexKeyUtil.create(CaosDefCommandElementsByNameIndex::class.java)
        @JvmStatic
        val Instance = CaosDefCommandElementsByNameIndex();
    }

}