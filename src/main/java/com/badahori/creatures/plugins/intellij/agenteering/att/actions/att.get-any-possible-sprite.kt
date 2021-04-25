package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes


internal fun getAnyPossibleSprite(project: Project, attFile: VirtualFile, spriteFileNameBase: String = attFile.nameWithoutExtension): VirtualFile? {
    var parent = attFile.parent
    var spriteFile: VirtualFile? = null
    while (spriteFile == null && parent != null) {
        spriteFile = searchParentRecursive(project, parent, spriteFileNameBase)
        parent = parent.parent
    }
    if (spriteFile != null)
        return spriteFile;
    val module = attFile.getModule(project)
        ?: return null
    val searchScope = GlobalSearchScope.moduleScope(module)
    return getVirtualFilesByName(project, spriteFileNameBase, "spr", searchScope).firstOrNull()
        ?: getVirtualFilesByName(project, spriteFileNameBase, "c16", searchScope).firstOrNull()
        ?: getVirtualFilesByName(project, spriteFileNameBase, "s16", searchScope).firstOrNull()
}


private fun searchParentRecursive(project: Project, parent: VirtualFile, spriteFile: String): VirtualFile? {
    getAnySpriteMatching(parent, spriteFile)?.let {
        return it
    }
    val searchScope = GlobalSearchScopes.directoriesScope(project, true, parent)
    return getVirtualFilesByName(project, spriteFile, "spr", searchScope).firstOrNull()
        ?: getVirtualFilesByName(project, spriteFile, "c16", searchScope).firstOrNull()
        ?: getVirtualFilesByName(project, spriteFile, "s16", searchScope).firstOrNull()
}

private fun getVirtualFilesByName(project: Project, spriteFile: String, extension:String, searchScope: GlobalSearchScope) : List<VirtualFile> {
    val rawFiles = (FilenameIndex.getAllFilesByExt(project, extension, searchScope) + FilenameIndex.getAllFilesByExt(project, extension.toUpperCase(), searchScope)).toSet()
    return rawFiles.filter {
        it.nameWithoutExtension like spriteFile
    }
}

private fun getAnySpriteMatching(parent: VirtualFile, spriteFile: String): VirtualFile? {
    return parent.findChild("$spriteFile.spr")
        ?: parent.findChild("$spriteFile.c16")
        ?: parent.findChild("$spriteFile.s16")
}