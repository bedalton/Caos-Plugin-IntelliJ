package com.badahori.creatures.plugins.intellij.agenteering.vfs

object CaosVirtualFileCollector {


    fun collect(): List<CaosVirtualFile> {
        return CaosVirtualFileSystem.instance.rootChildren.flatMap {
            collect(it)
        }
    }

    fun collectFilesWithExtension(extension: String): List<CaosVirtualFile> {
        return CaosVirtualFileSystem.instance.rootChildren.flatMap {child ->
            collectFilesWithExtension(child, extension)
        }
    }

    fun collectFilesWithExtension(file: CaosVirtualFile, extension: String): List<CaosVirtualFile> {
        return when {
            file.isDirectory -> file.children.flatMap { collectFilesWithExtension(file, extension) }
            file.extension == extension -> listOf(file)
            else -> emptyList()
        }
    }

    fun collect(file: CaosVirtualFile): List<CaosVirtualFile> {
        return if (file.isDirectory) {
            file.children.flatMap { collect(file) }
        } else
            listOf(file)
    }
}