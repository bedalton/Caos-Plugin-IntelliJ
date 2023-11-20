package com.badahori.creatures.plugins.intellij.agenteering.caos.scopes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.UNKNOWN
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.like
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.inferVariantHard
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope


/**
 * Search Scope to filter files by variant
 */
class CaosVariantSearchScope(
    private val myVariant: CaosVariant?,
    private val myProject: Project,
    private val strict: Boolean = true
) : SearchScope() {

    private val isC3DS by lazy {
        myVariant?.isC3DS == true
    }

    override fun intersectWith(otherScope: SearchScope): SearchScope {
        if (otherScope is GlobalSearchScope) {
            return MyGlobalSearchScope { file ->
                this.contains(file) && otherScope.contains(file)
            }
        }
        return MySearchScope { file ->
            this.contains(file) && otherScope.contains(file)
        }
    }

    override fun union(otherScope: SearchScope): SearchScope {
        if (otherScope is GlobalSearchScope) {
            return MyGlobalSearchScope { file ->
                this.contains(file) || otherScope.contains(file)
            }
        }
        return MySearchScope { file ->
            this.contains(file) || otherScope.contains(file)
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        val variant = (file as? CaosVirtualFile)?.variant
            ?: file.getPsiFile(myProject)?.variant
            ?: file.getModule(myProject)?.variant
        if (strict)
            return variant == myVariant
        if (isC3DS && variant?.isC3DS != false)
            return true
        return variant == myVariant || listOfNotNull(variant, myVariant).let { it.size < 2 || UNKNOWN in it }
    }

    override fun getDisplayName(): String {
        return AgentMessages.message("caos.search-scope.variant-search-scope")
    }
}

/**
 * Search Scope to filter files by variant
 */
class CaosVariantGlobalSearchScope(
    private val myProject: Project,
    private val myVariant: CaosVariant?,
    private val strict: Boolean = false,
    private val searchLibraries: Boolean = true
) : GlobalSearchScope(myProject) {

    private val isC3DS by lazy {
        myVariant?.isC3DS == true
    }

    override fun contains(file: VirtualFile): Boolean {
        val variant = (file as? CaosVirtualFile)?.variant
            ?: file.getPsiFile(myProject)?.variant
            ?: file.getModule(myProject)?.variant
            ?: project?.inferVariantHard()

        if (strict) {
            // False because we already checked if variants were equal
            return variant == myVariant
        }

        if (variant == myVariant || variant like myVariant) {
            return true
        }

        if (isC3DS && variant?.isC3DS != false) {
            return true
        }

        return variant == null || myVariant == null || variant == UNKNOWN || myVariant == UNKNOWN
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return true
    }

    override fun isSearchInLibraries(): Boolean {
        return searchLibraries
    }

    override fun getDisplayName(): String {
        return AgentMessages.message("caos.search-scope.global-variant-search-scope")
    }
}

private class MySearchScope(private val callback: (file: VirtualFile) -> Boolean): SearchScope() {
    override fun intersectWith(otherScope: SearchScope): SearchScope {
        if (otherScope is GlobalSearchScope) {
            return MyGlobalSearchScope { file ->
                callback(file) && otherScope.contains(file)
            }
        }
        return MySearchScope { file ->
            callback(file) && otherScope.contains(file)
        }
    }

    override fun union(otherScope: SearchScope): SearchScope {
        if (otherScope is GlobalSearchScope) {
            return MyGlobalSearchScope { file ->
                callback(file) || otherScope.contains(file)
            }
        }
        return MySearchScope { file ->
            callback(file) || otherScope.contains(file)
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return callback(file)
    }
}

/**
 * Simple class to combine to GlobalSearchScopes
 */
private class MyGlobalSearchScope(
    private val searchModule: (Module) -> Boolean = { true },
    private val searchLibraries: Boolean = true,
    private val callback: (file: VirtualFile) -> Boolean
): GlobalSearchScope() {

    override fun intersectWith(otherScope: SearchScope): GlobalSearchScope {
        if (otherScope is GlobalSearchScope) {
            return MyGlobalSearchScope { file ->
                callback(file) && otherScope.contains(file)
            }
        }
        return MyGlobalSearchScope { file ->
            callback(file) && otherScope.contains(file)
        }
    }

    override fun intersectWith(otherScope: GlobalSearchScope): GlobalSearchScope {
        return MyGlobalSearchScope { file ->
            callback(file) && otherScope.contains(file)
        }
    }

    override fun union(otherScope: SearchScope): GlobalSearchScope {
        if (otherScope is GlobalSearchScope) {
            return MyGlobalSearchScope { file ->
                callback(file) || otherScope.contains(file)
            }
        }
        return MyGlobalSearchScope { file ->
            callback(file) || otherScope.contains(file)
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return callback(file)
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return searchModule(aModule)
    }

    override fun isSearchInLibraries(): Boolean {
        return searchLibraries
    }
}