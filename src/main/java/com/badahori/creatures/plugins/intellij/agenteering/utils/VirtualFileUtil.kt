package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.vfs.VirtualFile

object VirtualFileUtil {

    fun childrenWithExtensions(virtualFile:VirtualFile, recursive:Boolean, vararg extensionsIn:String) : List<VirtualFile> {
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
    fun collectChildFiles(virtualFile:VirtualFile, recursive:Boolean) : List<VirtualFile> {
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

}