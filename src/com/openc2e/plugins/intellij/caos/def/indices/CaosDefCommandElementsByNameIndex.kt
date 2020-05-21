package com.openc2e.plugins.intellij.caos.def.indices

import com.intellij.psi.stubs.StubIndexKey
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.indices.CaosScriptCaseInsensitiveStringIndexBase
import com.openc2e.plugins.intellij.caos.indices.CaosStringIndexBase
import com.openc2e.plugins.intellij.caos.indices.IndexKeyUtil

class CaosDefCommandElementsByNameIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosDefCommandDefElement>(CaosDefCommandDefElement::class.java) {

    override fun getKey(): StubIndexKey<String, CaosDefCommandDefElement> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    companion object {
        private const val VERSION = 1
        @JvmStatic
        val KEY = IndexKeyUtil.create(CaosDefCommandElementsByNameIndex::class.java)
        @JvmStatic
        val Instance = CaosDefCommandElementsByNameIndex();
    }

}