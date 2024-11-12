package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.vfs.collectChildren
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes


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
    ProgressIndicatorProvider.checkCanceled()
    // Lowercase the extension
    val fileExtension = fileExtensionTemp.lowercase()

    val theSearchScope: GlobalSearchScope = searchScope
        ?: (if (virtualFile != null && virtualFile.isDirectory) GlobalSearchScopes.directoryScope(project, virtualFile, true) else if (virtualFile != null) GlobalSearchScopes.directoryScope(project, virtualFile.parent, true) else null)
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
    return try {
        FilenameIndex.getAllFilesByExt(project, fileExtension, theSearchScope).toList()
    } catch (e: Exception) {
        e.rethrowAnyCancellationException()
        if (virtualFile != null) {
            getFilesWithExtensionWithoutIndex(virtualFile, fileExtension).filter {
                theSearchScope.accept(it)
            }
        } else {
            emptyList()
        }
    }
}

/**
 * Gets files with extensions manually in a virtual file directory
 */
private fun getFilesWithExtensionWithoutIndex(virtualFile: VirtualFile, extension: String): List<VirtualFile> {
    if (!virtualFile.isDirectory) {
        return getFilesWithExtensionWithoutIndex(virtualFile.parent, extension)
    }
    return virtualFile.collectChildren {
        ProgressIndicatorProvider.checkCanceled()
        it.extension?.equals(extension, true) == true
    }
}