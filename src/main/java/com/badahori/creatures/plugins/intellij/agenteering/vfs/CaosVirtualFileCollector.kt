package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.search.SearchScope

object CaosVirtualFileCollector {


    fun collect(scope: SearchScope? = null): List<CaosVirtualFile> {
        ProgressIndicatorProvider.checkCanceled()
        return CaosVirtualFileSystem.instance.children.flatMap {
            collect(it, scope)
        }
    }

    fun collectFilesWithExtension(extension: String, scope: SearchScope? = null): List<CaosVirtualFile> {
        ProgressIndicatorProvider.checkCanceled()
        return CaosVirtualFileSystem.instance.children.flatMap {file ->
            if (file.isDirectory) {
                file.childrenAsList().flatMap { child -> collectFilesWithExtension(child, extension, scope) }
            } else if (scope == null || scope.contains(file))
                listOf(file)
            else
                emptyList()
        }
    }

    fun collectFilesWithExtension(file: CaosVirtualFile, extension: String, scope: SearchScope? = null): List<CaosVirtualFile> {
        ProgressIndicatorProvider.checkCanceled()
        return when {
            file.isDirectory -> file.childrenAsList().flatMap { child -> collectFilesWithExtension(child, extension, scope) }
            file.extension == extension -> if (scope == null || scope.contains(file)) listOf(file) else emptyList()
            else -> emptyList()
        }
    }

    fun collect(file: CaosVirtualFile, scope: SearchScope? = null): List<CaosVirtualFile> {
        ProgressIndicatorProvider.checkCanceled()
        return if (file.isDirectory) {
            file.childrenAsList().flatMap { child -> collect(child, scope) }
        } else if (scope == null || scope.contains(file))
            listOf(file)
        else
            emptyList()
    }
}