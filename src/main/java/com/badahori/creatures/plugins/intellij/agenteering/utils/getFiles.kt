package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope


/**
 * Gets files in module or project with extension if index is build, or in containing director
 */
internal fun getFilesWithExtension(
    project: Project,
    module: Module?,
    virtualFile: VirtualFile?,
    fileExtensionTemp: String,
    searchScope: GlobalSearchScope? = null
): List<VirtualFile> {
    // Lowercase the extension
    val fileExtension = fileExtensionTemp.lowercase()


    val theSearchScope: GlobalSearchScope = searchScope
        ?: module?.let { GlobalSearchScope.moduleScope(it) }
        ?: GlobalSearchScope.projectScope(project)

    // If service is done, do a manual search
    if (DumbService.isDumb(project)) {
        return if (virtualFile != null) {
            getFilesWithExtensionWithoutIndex(virtualFile, fileExtension).filter {
                theSearchScope.accept(it)
            }
        } else {
            emptyList()
        }
    }
    return FilenameIndex.getAllFilesByExt(project, fileExtension, theSearchScope).toList()
}

/**
 * Gets files with extensions manually in a virtual file directory
 */
private fun getFilesWithExtensionWithoutIndex(virtualFile: VirtualFile, extension: String): List<VirtualFile> {
    if (!virtualFile.isDirectory) {
        return getFilesWithExtensionWithoutIndex(virtualFile.parent, extension)
    }
    return virtualFile.children.flatMap {
        if (it.isDirectory) {
            getFilesWithExtensionWithoutIndex(it, extension)
        } else if (it.extension?.equals(extension, true) == true) {
            listOf(it)
        } else {
            emptyList()
        }
    }
}