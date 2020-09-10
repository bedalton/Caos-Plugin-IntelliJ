package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedVar

class CaosScriptEventScriptIndex : CaosStringIndexBase<CaosScriptEventScript>(CaosScriptEventScript::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptEventScript> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    companion object {
        val KEY: StubIndexKey<String, CaosScriptEventScript> = IndexKeyUtil.create(CaosScriptEventScriptIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptEventScriptIndex()
        fun toIndexKey(familyIn:Int, genusIn:Int, speciesIn:Int, eventNumberIn:Int) : String {
            val family = familyIn % 65535
            val genus = genusIn % 65535
            val species = speciesIn % 65535
            val eventNumber = eventNumberIn % 65535
            return "$family|$genus|$species|$eventNumber"
        }
    }

}