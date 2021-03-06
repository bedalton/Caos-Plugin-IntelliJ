package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefDocCommentHashtag
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosStringIndexBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.IndexKeyUtil

class CaosDefHashTagsIndex : CaosStringIndexBase<CaosDefDocCommentHashtag>(CaosDefDocCommentHashtag::class.java) {

    override fun getKey(): StubIndexKey<String, CaosDefDocCommentHashtag> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    companion object {
        private const val VERSION = 0
        @JvmStatic
        val KEY: StubIndexKey<String, CaosDefDocCommentHashtag> = IndexKeyUtil.create(CaosDefHashTagsIndex::class.java)
        val Instance = CaosDefHashTagsIndex()
    }

}