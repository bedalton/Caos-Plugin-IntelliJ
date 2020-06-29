package com.openc2e.plugins.intellij.agenteering.caos.indices

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.openc2e.plugins.intellij.agenteering.caos.stubs.CAOS_SCRIPT_STUB_VERSION
import com.openc2e.plugins.intellij.agenteering.caos.utils.startsAndEndsWith
import java.util.logging.Logger
import java.util.regex.Pattern

abstract class CaosStringIndexBase<PsiT : PsiElement>
/**
 * Const
 * @param elementClass the psi element class for this elements in this index
 */
internal constructor(private val indexedElementClass: Class<PsiT>) : StringStubIndexExtension<PsiT>() {

    override fun getVersion(): Int {
        return super.getVersion() + CAOS_SCRIPT_STUB_VERSION
    }

    protected fun getVersion(version:Int): Int {
        return getVersion() + version
    }

    open operator fun get(variableName: String, project: Project): List<PsiT> {
        return get(variableName, project, GlobalSearchScope.allScope(project))
    }

    override operator fun get(keyString: String, project: Project, scope: GlobalSearchScope): List<PsiT> {
        val keyLowercase = keyString.toLowerCase()
        return getAllKeys(project)
                .filter { it.toLowerCase() == keyLowercase }
                .flatMap {
                    StubIndex.getElements(key, it, project, scope, indexedElementClass)
                }
    }

    fun getAllInScope(project: Project, scope: GlobalSearchScope) : List<PsiT> {
        return getAllKeys(project)
                .flatMap { key ->
                    get(key, project, scope)
                }
    }

    open fun getByPattern(start: String?, tail: String?, project: Project): Map<String, List<PsiT>> {
        return getByPattern(start, tail, project, null)
    }

    @Suppress("SameParameterValue")
    private fun getByPattern(
            start: String?,
            tail: String?,
            project: Project,
            globalSearchScope: GlobalSearchScope?): Map<String, List<PsiT>> {

        val keys = ArrayList<String>()
        val notMatchingKeys = ArrayList<String>()
        for (key in getAllKeys(project)) {
            if (notMatchingKeys.contains(key) || keys.contains(key)) {
                continue
            }
            if (key.toLowerCase().startsAndEndsWith(start?.toLowerCase(), tail?.toLowerCase())) {
                keys.add(key)
            } else {
                notMatchingKeys.add(key)
            }
        }
        return getAllForKeys(keys, project, globalSearchScope)

    }

    open fun getByPattern(patternString: String?, project: Project): Map<String, List<PsiT>> {
        return getByPattern(patternString, project, null)
    }

    open fun getByPattern(patternString: String?, project: Project, globalSearchScope: GlobalSearchScope?): Map<String, List<PsiT>> {
        return if (patternString == null) {
            emptyMap()
        } else getAllForKeys(getKeysByPattern(patternString, project, globalSearchScope), project, globalSearchScope)
    }

    @JvmOverloads
    protected fun getAllForKeys(keys: List<String>, project: Project, globalSearchScope: GlobalSearchScope? = null): Map<String, MutableList<PsiT>> {
        val out = HashMap<String, MutableList<PsiT>>() as MutableMap<String, MutableList<PsiT>>
        for (key in keys) {
            if (out.containsKey(key)) {
                out[key]!!.addAll(get(key, project, globalSearchScope ?: GlobalSearchScope.allScope(project)))
            } else {
                out[key] = get(key, project, globalSearchScope ?: GlobalSearchScope.allScope(project)).toMutableList()
            }
        }
        return out
    }

    open fun containsKey(key:String, project: Project) : Boolean {
        return getAllKeys(project).contains(key)
    }

    open fun getStartingWith(pattern: String, project: Project): List<PsiT> {
        return getByPatternFlat("$pattern(.*)", project)
    }

    @JvmOverloads
    open fun getByPatternFlat(pattern: String, project: Project, scope: GlobalSearchScope? = null): List<PsiT> {
        val keys = getKeysByPattern(pattern, project, scope)
        return getAllForKeysFlat(keys, project, scope)

    }

    protected fun getAllForKeysFlat(keys: List<String>, project: Project, globalSearchScope: GlobalSearchScope?): List<PsiT> {
        val out = ArrayList<PsiT>()
        val done = ArrayList<String>()
        for (key in keys) {
            if (!done.contains(key)) {
                done.add(key)
                out.addAll(get(key, project, scopeOrDefault(globalSearchScope, project)))
            }
        }
        return out
    }

    @JvmOverloads
    open fun getKeysByPattern(patternString: String?, project: Project, @Suppress("UNUSED_PARAMETER") globalSearchScope: GlobalSearchScope? = null): List<String> {
        if (patternString == null) {
            return emptyList()
        }
        val matchingKeys = ArrayList<String>()
        val notMatchingKeys = ArrayList<String>()
        val pattern: Pattern = try {
            Pattern.compile(patternString)
        } catch (e: Exception) {
            Pattern.compile(Pattern.quote(patternString))
        }

        for (key in getAllKeys(project)) {
            if (notMatchingKeys.contains(key) || matchingKeys.contains(key)) {
                continue
            }
            if (pattern.matcher(key).matches()) {
                matchingKeys.add(key)
            } else {
                notMatchingKeys.add(key)
            }
        }
        return matchingKeys
    }

    @JvmOverloads
    fun getAll(project: Project, globalSearchScope: GlobalSearchScope? = null): List<PsiT> {
        val out = ArrayList<PsiT>()
        for (key in getAllKeys(project)) {
            out.addAll(get(key, project, scopeOrDefault(globalSearchScope, project)))
        }
        return out
    }

    internal fun scopeOrDefault(scope : GlobalSearchScope?, project: Project) : GlobalSearchScope {
        return scope ?: GlobalSearchScope.allScope(project)
    }

    companion object {

        @Suppress("unused")
        private val LOGGER by lazy {
            Logger.getLogger(CaosStringIndexBase::class.java.name)
        }
        protected const val VERSION = 1
    }

}
