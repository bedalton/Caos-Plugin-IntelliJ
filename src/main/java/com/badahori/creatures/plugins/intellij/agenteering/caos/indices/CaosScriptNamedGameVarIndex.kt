@file:Suppress("DeprecatedCallableAddReplaceWith")

package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey

class CaosScriptNamedGameVarIndex : CaosStringIndexBase<CaosScriptNamedGameVar>(CaosScriptNamedGameVar::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptNamedGameVar> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    @Deprecated("Use Use get with CaosScriptNamedGameVarType;\n" +
            "get(type:CaosScriptNamedGameVarType, key:String, project:Project)"
    )
    override fun get(variableName: String, project: Project): List<CaosScriptNamedGameVar> {
        return super.get(variableName, project)
    }

    @Deprecated(
        "Use get with CaosScriptNamedGameVarType;\n" +
                "get(type:CaosScriptNamedGameVarType, key:String, project:Project, scope: GlobalSearchScope)"
    )
    override fun get(keyString: String, project: Project, scope: GlobalSearchScope): List<CaosScriptNamedGameVar> {
        return super.get(keyString, project, scope)
    }

    operator fun get(type:CaosScriptNamedGameVarType, key:String, project:Project) : List<CaosScriptNamedGameVar> {
        val indexKey = getKey(type, key)
        @Suppress("DEPRECATION")
        return get(indexKey, project)
    }

    operator fun get(type:CaosScriptNamedGameVarType, key:String, project:Project, globalSearchScope: GlobalSearchScope) : List<CaosScriptNamedGameVar> {
        val indexKey = getKey(type, key)
        @Suppress("DEPRECATION")
        return get(indexKey, project, globalSearchScope)
    }

    companion object {
        val KEY: StubIndexKey<String, CaosScriptNamedGameVar> = IndexKeyUtil.create(CaosScriptNamedGameVarIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptNamedGameVarIndex()

        fun getKey(type:CaosScriptNamedGameVarType, key:String) : String {
            return type.token + ":" + key
        }
    }
}


