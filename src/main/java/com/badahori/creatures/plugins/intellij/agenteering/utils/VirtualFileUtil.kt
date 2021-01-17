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

}