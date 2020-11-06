package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.intellij.psi.search.SearchScope

object CaosVirtualFileCollector {


    fun collect(): List<CaosVirtualFile> {
        return CaosVirtualFileSystem.instance.children.flatMap {
            collect(it)
        }
    }

    fun collectFilesWithExtension(extension: String, scope:SearchScope? = null): List<CaosVirtualFile> {
        return CaosVirtualFileSystem.instance.children.flatMap {child ->
            collectFilesWithExtension(child, extension, scope)
        }
    }

    fun collectFilesWithExtension(file: CaosVirtualFile, extension: String, scope:SearchScope? = null): List<CaosVirtualFile> {
        if (scope?.contains(file) == false)
            return emptyList()
        return when {
            file.isDirectory -> file.childrenAsList().flatMap { child-> collectFilesWithExtension(child, extension) }
            file.extension == extension -> listOf(file)
            else -> emptyList()
        }
    }

    fun collect(file: CaosVirtualFile): List<CaosVirtualFile> {
        return if (file.isDirectory) {
            file.childrenAsList().flatMap { child -> collect(child) } + file
        } else
            listOf(file)
    }
}