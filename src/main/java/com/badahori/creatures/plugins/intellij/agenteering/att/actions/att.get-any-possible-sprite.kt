package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.scopes.CaosVariantGlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes


/**
 * Gets any sprite matching for this att file in this project.
 * Searches from ATT file directory up directory hierarchy
 * then module anywhere in the module
 * then anywhere in the project
 */
internal fun getAnyPossibleSprite(project: Project, attFile: VirtualFile, spriteFileNameBase: String = attFile.nameWithoutExtension): VirtualFile? {
    if (project.isDisposed) {
        return null
    }
    var parent = attFile.parent
    val module = attFile.getModule(project)

    // Init search scope
    val searchScope = module?.moduleContentScope ?:  GlobalSearchScope.projectScope(project)
    // Get variant
    val variant = getInitialVariant(project, attFile).nullIfUnknown()
    // Add variant search scope
    val variantSearchScope = if (variant != null) {
        searchScope.intersectWith(CaosVariantGlobalSearchScope(project, variant))
    } else {
        searchScope
    }
    val spriteFileName = if (spriteFileNameBase[0] == 'z') {
        'a' + spriteFileNameBase.substring(1)
    } else {
        spriteFileNameBase
    }
    var spriteFile: VirtualFile? = null
    while (spriteFile == null && parent != null) {
        spriteFile = searchParentRecursive(project, parent, spriteFileName, variantSearchScope)
        parent = parent.parent
    }
    if (spriteFile != null)
        return spriteFile

    if (DumbService.isDumb(project)) {
        return null
    }
    return BreedSpriteIndex.findMatching(project, spriteFileName, variantSearchScope, null)
        .firstOrNull()
        ?: BreedSpriteIndex.findMatching(project, spriteFileName, searchScope, null)
            .firstOrNull()
}

/**
 * Searches parent directory for any matching children
 */
private fun searchParentRecursive(project: Project, parent: VirtualFile, spriteFile: String, scope: GlobalSearchScope? = null): VirtualFile? {
    getAnySpriteMatching(parent, spriteFile)?.let {
        return it
    }
    val searchScope = GlobalSearchScopes.directoriesScope(project, true, parent).let {
        if (scope != null)
            it.intersectWith(scope)
        else
            it
    }
    return getVirtualFilesByName(project, spriteFile, "spr", searchScope).firstOrNull()
        ?: getVirtualFilesByName(project, spriteFile, "c16", searchScope).firstOrNull()
        ?: getVirtualFilesByName(project, spriteFile, "s16", searchScope).firstOrNull()
}

/**
 * Gets any kind of sprite with name in parent directory, non-recursive
 */
private fun getAnySpriteMatching(parent: VirtualFile, spriteFile: String): VirtualFile? {
    return parent.findChild("$spriteFile.spr")
        ?: parent.findChild("$spriteFile.c16")
        ?: parent.findChild("$spriteFile.s16")
}

/**
 * Finds the virtual file for a sprite file based on name, scope and extension
 * Must do this case insensitively
 */
private fun getVirtualFilesByName(project: Project, spriteFile: String, extension:String, searchScope: GlobalSearchScope) : List<VirtualFile> {
    val rawFiles = (FilenameIndex.getAllFilesByExt(project, extension, searchScope) + FilenameIndex.getAllFilesByExt(project, extension.uppercase(), searchScope)).toSet()
    return rawFiles.filter {
        it.nameWithoutExtension like spriteFile
    }
}