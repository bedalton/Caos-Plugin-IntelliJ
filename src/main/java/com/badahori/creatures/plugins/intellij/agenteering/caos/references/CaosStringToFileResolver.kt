package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.stringTextToAbsolutePath
import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.toListOf
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.io.File

internal object CaosStringToFileResolver {

    /**
     * Takes a string like PSI element and resolves it to its referenced file(s)
     */
    internal fun resolveToFiles(
        project: Project,
        element: CaosScriptStringLike,
        searchScope: GlobalSearchScope = GlobalSearchScope.projectScope(project)
    ): Collection<VirtualFile>? {


        // Ensure this string can be resolved
        val stubKind = element
            .stringStubKind
            ?: return null

        // Make sure it can be resolved to a file
        if (!stubKind.isFile) {
            return null
        }

        // Get possible extensions
        val extensions = stubKind
            .extensions
            .nullIfEmpty()
            ?: return null


        // Get base name components
        val relativePath = element.stringValue
            // Remove current directory dot
            .replace("^(\\./)+".toRegex(), "")
        val relativePathLowercase = relativePath.lowercase()
        val isFullName = extensions.any { relativePathLowercase.endsWith(it.lowercase()) }

        // Determine if this path is relative
        val isRelative = isRelative(relativePath)

        // Search by full name or relative path
        if (isFullName || isRelative) {
            return getByFullName(project, element, isRelative, searchScope)
                .nullIfEmpty()
        }

        return CaseInsensitiveFileIndex
            .findWithFileNameAndExtensions(
                project,
                relativePath, // checked so no extension && no path components
                extensions.map { it.lowercase() }.toSet(),
                searchScope
            )
    }

    /**
     * Searches for a files based on a full name, with extension
     */
    private fun getByFullName(
        project: Project,
        element: CaosScriptStringLike,
        isRelative: Boolean,
        searchScope: GlobalSearchScope = GlobalSearchScope.projectScope(project)
    ): Collection<VirtualFile>? {

        // Find by relative path first
        // Should help if  file is in same folder as file with name
        val firstResolve = resolveToFileRelative(element)
            ?.toListOf()

        // If path is relative, or a match already found return it
        if (isRelative || firstResolve.isNotNullOrEmpty()) {
            return firstResolve
        }

        // Get basename for search
        val basename = PathUtil.getLastPathComponent(element.stringValue)
            ?: return null

        // Return any matches
        return CaseInsensitiveFileIndex
            .findWithFileName(project, basename, searchScope)
    }

    /**
     * Resolves a relative path in a string to a virtual file
     * This is made possible by using the element's containing file's parent
     */
    private fun resolveToFileRelative(element: CaosScriptStringLike): VirtualFile? {
        val absolutePath = element.stringTextToAbsolutePath()
            ?: return null
        val myElementVirtualFile = element.virtualFile
        if (myElementVirtualFile is CaosVirtualFile) {
            return CaosVirtualFileSystem.instance.findFileByPath(absolutePath)
        }
        val file = File(absolutePath)
        return if (file.exists()) {
            VfsUtil.findFileByIoFile(file, true)
        } else {
            null
        }
    }

    /**
     * Checks if a path has additional path components
     */
    private fun isRelative(path: String): Boolean {
        // Replace known escapes for single and double quotes
        val unescaped = path
            .replace("^\\./".toRegex(), "") // remove current directory dot
            .replace("\\\\(['\"^])".toRegex(), "$1")
        return unescaped.contains("\\") || unescaped.contains('/')
    }
}