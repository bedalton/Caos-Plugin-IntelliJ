package com.badahori.creatures.plugins.intellij.agenteering.caos.scopes

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.SearchScope


/**
 * Search Scope to filter files by variant
 */
class CaosVariantSearchScope constructor(private val myVariant: CaosVariant?, private val project:Project, private val strict:Boolean = true) : SearchScope() {

    override fun intersectWith(otherScope: SearchScope): SearchScope {
        return IntersectSearchScope(this, otherScope)
    }

    override fun union(otherScope: SearchScope): SearchScope {
        return UnionSearchScope(this, otherScope)
    }

    override fun contains(file: VirtualFile): Boolean {
        val variant = (file as? CaosVirtualFile)?.variant
                ?: file.getPsiFile(project)?.variant
                ?: file.getModule(project)?.variant
        if (strict)
            return variant == myVariant
        return variant == myVariant || listOfNotNull(variant, myVariant).let { it.size < 2 || CaosVariant.UNKNOWN in it }
    }

    override fun getDisplayName(): String {
        return "CAOS Variant Search Scope"
    }
}

/**
 * Simple class to combine to search scopes by Union
 */
private class UnionSearchScope(private val searchScope1:SearchScope, private val searchScope2:SearchScope) : SearchScope() {
    override fun intersectWith(otherScope: SearchScope): SearchScope {
        return IntersectSearchScope(this, otherScope)
    }

    override fun union(otherScope: SearchScope): SearchScope {
        return UnionSearchScope(this, otherScope)
    }

    override fun contains(file: VirtualFile): Boolean {
        return searchScope1.contains(file) || searchScope2.contains(file)
    }

}

/**
 * Simple class to combine to search scopes by intersection
 */
internal class IntersectSearchScope(private val searchScope1:SearchScope, private val searchScope2:SearchScope) : SearchScope() {
    override fun intersectWith(otherScope: SearchScope): SearchScope {
        return IntersectSearchScope(this, otherScope)
    }

    override fun union(otherScope: SearchScope): SearchScope {
        return UnionSearchScope(this, otherScope)
    }

    override fun contains(file: VirtualFile): Boolean {
        return searchScope1.contains(file) && searchScope2.contains(file)
    }

}