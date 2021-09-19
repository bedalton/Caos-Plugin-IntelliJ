package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.scopes.CaosVariantGlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey

class CaosScriptEventScriptIndex : CaosStringIndexBase<CaosScriptEventScript>(CaosScriptEventScript::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptEventScript> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION


    /**
     * Get event scripts with classifier regardless of variant
     */
    operator fun get(
        familyIn: Int,
        genusIn: Int,
        speciesIn: Int,
        eventNumberIn: Int,
        project: Project
    ): Collection<CaosScriptEventScript> {
        return get(variant = null, familyIn, genusIn, speciesIn, eventNumberIn, project)
    }

    /**
     * Get event scripts according to variant and classifier
     */
    operator fun get(
        variant: CaosVariant?,
        familyIn: Int,
        genusIn: Int,
        speciesIn: Int,
        eventNumberIn: Int,
        project: Project
    ): Collection<CaosScriptEventScript> {
        if (familyIn != 0 && genusIn != 0 && speciesIn != 0 && eventNumberIn != 0)
            return get(toIndexKey(familyIn, genusIn, speciesIn, eventNumberIn), project)
        val wildCard = "\\d+"
        val family = if (familyIn == 0)
            wildCard
        else
            familyIn

        val genus = if (genusIn == 0)
            wildCard
        else
            genusIn

        val species = if (speciesIn == 0)
            wildCard
        else
            speciesIn

        val eventNumber = if (eventNumberIn == 0)
            wildCard
        else
            eventNumberIn

        val key = createRegexKey(family, genus, species, eventNumber)

        val scope: GlobalSearchScope? = if (variant != null)
            CaosVariantGlobalSearchScope(project, variant, strict = false, searchLibraries = true)
        else
            null

        return getByPattern(key, project, scope)
            .flatMap { it.value }
    }

    companion object {
        val KEY: StubIndexKey<String, CaosScriptEventScript> = IndexKeyUtil.create(CaosScriptEventScriptIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptEventScriptIndex()
        fun toIndexKey(familyIn: Int, genusIn: Int, speciesIn: Int, eventNumberIn: Int) : String {
            val family = familyIn % 65535
            val genus = genusIn % 65535
            val species = speciesIn % 65535
            val eventNumber = eventNumberIn % 65535
            return "$family|$genus|$species|$eventNumber"
        }

        private fun createRegexKey(family: Any, genus: Any, species: Any, eventNumber: Any): String {
            return "$family\\|$genus\\|$species\\|$eventNumber"
        }
    }

}