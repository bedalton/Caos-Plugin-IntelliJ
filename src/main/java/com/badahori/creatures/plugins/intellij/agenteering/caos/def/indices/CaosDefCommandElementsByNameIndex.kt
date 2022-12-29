package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubVersions
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptCaseInsensitiveStringIndexBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.IndexKeyUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.StubIndexKey

class CaosDefCommandElementsByNameIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosDefCommandDefElement>(CaosDefCommandDefElement::class.java) {

    override fun getKey(): StubIndexKey<String, CaosDefCommandDefElement> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + CaosDefStubVersions.STUB_VERSION + VERSION
    }

    companion object {
        private const val VERSION = 2
        @JvmStatic
        val KEY: StubIndexKey<String, CaosDefCommandDefElement> = IndexKeyUtil.create(CaosDefCommandElementsByNameIndex::class.java)
        @JvmStatic
        val Instance = CaosDefCommandElementsByNameIndex()
    }

}