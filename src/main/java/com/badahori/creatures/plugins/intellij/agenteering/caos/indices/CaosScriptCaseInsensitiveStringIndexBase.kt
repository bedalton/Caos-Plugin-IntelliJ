package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.badahori.creatures.plugins.intellij.agenteering.utils.startsAndEndsWith
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import java.util.logging.Logger
import java.util.regex.Pattern

abstract class CaosScriptCaseInsensitiveStringIndexBase<PsiT : PsiElement>
/**
 * Const
 * @param indexedElementClass the psi element class for the elements in this index
 */
internal constructor(private val indexedElementClass: Class<PsiT>) : StringStubIndexExtension<PsiT>() {

    override fun getVersion(): Int {
        return super.getVersion() + VERSION
    }

    protected fun getVersion(version:Int): Int {
        return getVersion() + version
    }

    open operator fun get(key: String, project: Project): List<PsiT> {
        return getAllKeys(project).filter { it.equals(key, true) }.flatMap {
            ProgressIndicatorProvider.checkCanceled()
            get(it, project, GlobalSearchScope.projectScope(project))
        }
    }

    override operator fun get(key: String, project: Project, scope: GlobalSearchScope): List<PsiT> {
        return getAllKeys(project).filter { it.equals(key, true) }.flatMap {
            ProgressIndicatorProvider.checkCanceled()
            StubIndex.getElements(this.key, it, project, scope, indexedElementClass).toList()
        }
    }


    override fun getAllKeys(project: Project): MutableCollection<String> {
        return getAllKeys(project, null)
    }

    fun getAllKeysEverythingScope(project: Project): MutableCollection<String> {
        return super.getAllKeys(project)
    }

    override fun traceKeyHashToVirtualFileMapping(): Boolean {
        return true
    }
    fun getAllKeys(project: Project, scope: GlobalSearchScope?): MutableCollection<String> {
        var actualScope = GlobalSearchScope.projectScope(project)
        if (scope != null) {
            actualScope = scope.intersectWith(scope)
        }
        val out = mutableListOf<String>()
        StubIndex.getInstance().processAllKeys(
            key,
            process@{ key ->
                out.add(key)
                return@process true
            },
            actualScope,
            null
        )
        return out
    }

    open fun getByPattern(start: String?, tail: String?, project: Project): Map<String, List<PsiT>> {
        return getByPattern(start, tail, project, null)
    }

    @Suppress("SameParameterValue")
    private fun getByPattern(
            startIn: String?,
            tailIn: String?,
            project: Project,
            globalSearchScope: GlobalSearchScope?): Map<String, List<PsiT>> {
        val start = startIn?.lowercase()
        val tail = tailIn?.lowercase()
        val keys = ArrayList<String>()
        val notMatchingKeys = ArrayList<String>()
        for (keyIn in getAllKeys(project)) {
            ProgressIndicatorProvider.checkCanceled()
            val key = keyIn.lowercase()
            if (notMatchingKeys.contains(key) || keys.contains(key)) {
                continue
            }
            if (key.startsAndEndsWith(start, tail)) {
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
            ProgressIndicatorProvider.checkCanceled()
            if (out.containsKey(key)) {
                out[key]!!.addAll(get(key, project, globalSearchScope ?: GlobalSearchScope.projectScope(project)))
            } else {
                out[key] = get(key, project, globalSearchScope ?: GlobalSearchScope.projectScope(project)).toMutableList()
            }
        }
        return out
    }

    open fun containsKey(key:String, project: Project) : Boolean {
        return getAllKeys(project).map { it.lowercase() }.contains(key.lowercase())
    }

    open fun getStartingWith(pattern: String, project: Project): List<PsiT> {
        return getByPatternFlat("$pattern(.*)", project)
    }

    @JvmOverloads
    open fun getByPatternFlat(pattern: String, project: Project, scope: GlobalSearchScope? = null): List<PsiT> {
        val keys = getKeysByPattern(pattern, project, scope)
        return getAllForKeysFlat(keys, project, scope)

    }

    private fun getAllForKeysFlat(keys: List<String>, project: Project, globalSearchScope: GlobalSearchScope?): List<PsiT> {
        val out = ArrayList<PsiT>()
        val done = ArrayList<String>()
        for (key in keys) {
            ProgressIndicatorProvider.checkCanceled()
            if (!done.contains(key)) {
                done.add(key)
                out.addAll(get(key, project, scopeOrDefault(globalSearchScope, project)))
            }
        }
        return out
    }

    @JvmOverloads
    open fun getKeysByPattern(patternString: String?, project: Project, globalSearchScope: GlobalSearchScope? = null): List<String> {
        if (patternString == null) {
            return emptyList()
        }
        val matchingKeys = ArrayList<String>()
        val notMatchingKeys = ArrayList<String>()
        val pattern: Pattern = try {
            Pattern.compile(patternString.lowercase())
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            Pattern.compile(Pattern.quote(patternString.lowercase()))
        }
        for (key in getAllKeys(project)) {
            val keyToTest = key.lowercase()
            if (notMatchingKeys.contains(key) || matchingKeys.contains(key)) {
                continue
            }
            if (pattern.matcher(keyToTest).matches()) {
                matchingKeys.add(key)
            } else {
                notMatchingKeys.add(key)
            }
        }
        return matchingKeys
    }

    @JvmOverloads
    open fun getAll(project: Project, globalSearchScope: GlobalSearchScope? = null): List<PsiT> {
        val out = ArrayList<PsiT>()
        for (key in getAllKeys(project)) {
            ProgressIndicatorProvider.checkCanceled()
            out.addAll(get(key, project, scopeOrDefault(globalSearchScope, project)))
        }
        return out
    }

    private fun scopeOrDefault(scope : GlobalSearchScope?, project: Project) : GlobalSearchScope {
        return scope ?: GlobalSearchScope.projectScope(project)
    }

    companion object {

        @Suppress("unused")
        private val LOGGER by lazy {
            Logger.getLogger(CaosScriptCaseInsensitiveStringIndexBase::class.java.name)
        }
        protected const val VERSION = 1
    }

}
