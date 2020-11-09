package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.StubIndexKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptCaseInsensitiveStringIndexBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.IndexKeyUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.CaosDefElementsSearchExecutor
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class CaosDefCommandElementsByNameIndex : CaosScriptCaseInsensitiveStringIndexBase<CaosDefCommandDefElement>(CaosDefCommandDefElement::class.java) {

    override fun getKey(): StubIndexKey<String, CaosDefCommandDefElement> = KEY

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    override fun getAll(project: Project, globalSearchScope: GlobalSearchScope?): List<CaosDefCommandDefElement> {
        return super.getAll(project, globalSearchScope)/* + CaosDefElementsSearchExecutor
                .getCaosDefFiles(project)
                .collectElementsOfType(CaosDefCommandDefElement::class.java)*/
    }

    override fun get(keyIn: String, project: Project): List<CaosDefCommandDefElement> {
        return super.get(keyIn, project) /*+ CaosDefElementsSearchExecutor
                .getCaosDefFiles(project)
                .collectElementsOfType(CaosDefCommandDefElement::class.java)
                .filter{it.commandName like keyIn }*/
    }

    override fun get(keyIn: String, project: Project, scope: GlobalSearchScope): List<CaosDefCommandDefElement> {
        return super.get(keyIn, project, scope) /*+ CaosDefElementsSearchExecutor
                .getCaosDefFiles(project, scope)
                .collectElementsOfType(CaosDefCommandDefElement::class.java)
                .filter{ it.commandName like keyIn }*/
    }

    override fun getAllKeys(project: Project?): MutableCollection<String> {
        return (super.getAllKeys(project)) /*+ project?.let {CaosDefElementsSearchExecutor
                .getCaosDefFiles(project)
                .collectElementsOfType(CaosDefCommandDefElement::class.java)
                .map {
                    it.commandName
                }}.orEmpty()).toMutableList()*/
    }

    companion object {
        private const val VERSION = 2
        @JvmStatic
        val KEY: StubIndexKey<String, CaosDefCommandDefElement> = IndexKeyUtil.create(CaosDefCommandElementsByNameIndex::class.java)
        @JvmStatic
        val Instance = CaosDefCommandElementsByNameIndex();
    }

}