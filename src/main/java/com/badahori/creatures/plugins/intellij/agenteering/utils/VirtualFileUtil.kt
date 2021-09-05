package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.vfs.VirtualFile

object VirtualFileUtil {

    fun childrenWithExtensions(
        virtualFile: VirtualFile,
        recursive: Boolean,
        vararg extensionsIn: String
    ): List<VirtualFile> {
        val extensions = extensionsIn.toList()
        if (!recursive) {
            return virtualFile.children.filter {
                it.extension likeAny extensions
            }
        }
        return virtualFile.children.flatMap {
            if (it.isDirectory) {
                childrenWithExtensions(it, true, *extensionsIn)
            } else if (it.extension likeAny extensions) {
                listOf(it)
            } else
                emptyList()
        }
    }

    /**
     * Collects non-directory child files within a virtual file directory
     */
    fun collectChildFiles(virtualFile: VirtualFile, recursive: Boolean): List<VirtualFile> {
        if (!recursive) {
            return virtualFile.children.filterNot { it.isDirectory }
        }
        return virtualFile.children.flatMap {
            if (it.isDirectory) {
                collectChildFiles(it, true)
            } else {
                listOf(it)
            }
        }
    }

    fun findChildIgnoreCase(
        virtualFile: VirtualFile?,
        ignoreExtension: Boolean = false,
        vararg path: String
    ): VirtualFile? {

        val components = (if (path.any { it.contains("\\") })
            path.flatMap { it.split("\\") }
        else
            path.flatMap { it.split("/") }).filter { it.isNotNullOrBlank() }

        var file = virtualFile
            ?: return null

        for (component in components.dropLast(1)) {
            if (component == ".")
                continue
            if (component == "..")
                file = file.parent
                    ?: return null
            file = if (ignoreExtension) {
                file.children?.firstOrNull { it.nameWithoutExtension.equals(component, true) }
            } else {
                file.children?.firstOrNull { it.name.equals(component, true) }
            } ?: return null
        }

        return if (ignoreExtension) {
            val last = components.last()
            file.children?.firstOrNull { it.nameWithoutExtension.equals(FileNameUtils.getBaseName(last), true) }
        } else {
            file.children?.firstOrNull { it.name.equals(components.last(), true) }
        }
    }

    fun findChildrenIfDirectoryOrSiblingsIfLeaf(virtualFile: VirtualFile?, vararg path: String): List<VirtualFile>? {
        if (virtualFile == null)
            return null
        val components = if (path.any { it.contains("\\") })
            path.flatMap { it.split("\\") }
        else
            path.flatMap { it.split("/") }
        if (components.isEmpty()) {
            return null
        }
        if (components.size == 1) {
            val onlyComponent = components[0]
            if (FileNameUtils.getExtension(onlyComponent)?.nullIfEmpty() == null) {
                virtualFile.children.firstOrNull { it.name.equals(onlyComponent, ignoreCase = true) }
                    ?.let {
                        if (it.isDirectory)
                            return it.children.toList()
                    }
            }
            return if (virtualFile.isDirectory)
                virtualFile.children.toList()
            else
                virtualFile.parent?.children?.toList()

        }

        var file:VirtualFile = virtualFile
        for (component in components.dropLast(1)) {

            if (component == ".")
                continue

            if (component == "..")
                file = file.parent
                    ?: return null

            file = file.children?.firstOrNull { it.name.equals(component, true) }
                ?: return null
        }
        val lastComponent = components.last()
        var last = file.children?.firstOrNull { it.name.equals(lastComponent, true) }
        if (last == null) {
            if (FileNameUtils.getExtension(lastComponent).nullIfEmpty() == null)
                return null
            else
                last = file
        }
        return if (last.isDirectory) {
            last.children
        } else {
            file.children
        }.filter { !it.isDirectory }
    }

}