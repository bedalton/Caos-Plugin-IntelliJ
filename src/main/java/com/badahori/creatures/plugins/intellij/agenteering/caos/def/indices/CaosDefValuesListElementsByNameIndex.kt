package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubVersions
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptCaseInsensitiveStringIndexBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.IndexKeyUtil

class CaosDefValuesListElementsByNameIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosDefValuesListElement>(CaosDefValuesListElement::class.java) {

    override fun getKey(): StubIndexKey<String, CaosDefValuesListElement> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + CaosDefStubVersions.STUB_VERSION + VERSION
    }

    companion object {
        private const val VERSION = 2
        @JvmStatic
        val KEY: StubIndexKey<String, CaosDefValuesListElement> = IndexKeyUtil.create(CaosDefValuesListElementsByNameIndex::class.java)

        @JvmStatic
        val Instance = CaosDefValuesListElementsByNameIndex()
    }

}