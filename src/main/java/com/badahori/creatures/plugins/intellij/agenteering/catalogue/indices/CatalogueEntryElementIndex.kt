package com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptCaseInsensitiveStringIndexBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.IndexKeyUtil
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueEntryElement
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types.CatalogueStubVersions
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StubIndexKey

class CatalogueEntryElementIndex :
    CaosScriptCaseInsensitiveStringIndexBase<CatalogueEntryElement<*, *>>(CatalogueEntryElement::class.java) {

    override fun getKey(): StubIndexKey<String, CatalogueEntryElement<*, *>> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + CatalogueStubVersions.STUB_VERSION + VERSION
    }

    companion object {
        private const val VERSION = 1

        @JvmStatic
        val KEY: StubIndexKey<String, CatalogueEntryElement<*, *>> =
            IndexKeyUtil.create(CatalogueEntryElementIndex::class.java)

        @JvmStatic
        val Instance = CatalogueEntryElementIndex()
    }

}