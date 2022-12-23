@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor


internal fun VirtualFile.collectChildren(filter: (VirtualFile) -> Boolean = { true }): List<VirtualFile> {
    if (!this.isDirectory) {
        return emptyList()
    }
    val out = mutableListOf<VirtualFile>()
    VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Void?>() {
        // good
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                if (filter(file)) {
                    out.add(file)
                }
            }
            return true
        }
    })
    return out
}

internal fun <T> VirtualFile.collectChildrenAs(transform: (VirtualFile) -> T? ): List<T> {
    if (!this.isDirectory) {
        return emptyList()
    }
    val out = mutableListOf<T>()
    VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Void?>() {
        // good
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                transform(file)?.let {
                    out.add(it)
                }
            }
            return true
        }
    })
    return out
}