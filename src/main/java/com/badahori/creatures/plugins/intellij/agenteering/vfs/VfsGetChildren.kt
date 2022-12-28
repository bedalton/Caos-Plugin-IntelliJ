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

/**
 * Collects children in virtual file, transforming them as needed
 */
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

/**
 * Collects children in virtual file, transforming them as needed
 */
internal fun <T> VirtualFile.collectChildrenAsNullable(transform: (VirtualFile) -> T? ): List<T?> {
    if (!this.isDirectory) {
        return emptyList()
    }
    val out = mutableListOf<T?>()
    VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Void?>() {
        // good
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                out.add(transform(file))
            }
            return true
        }
    })
    return out
}


/**
 * Checks that all children within a virtual file directory match the predicate
 */
internal fun VirtualFile.allChildrenMatch(check: (VirtualFile) -> Boolean): Boolean {
    if (!this.isDirectory) {
        return false
    }
    var okay = true

    // Loop through children
    // Use this helper method to prevent infinite recursion
    VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Void?>() {
        // Check each file to see if it matches predicate
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                if (!check(file)) {
                    okay = false
                }
            }
            // As long as all files match, continue searching through children
            return okay
        }
    })
    return okay
}

/**
 * Checks that any child within a virtual file directory matches the predicate
 */
internal fun VirtualFile.anyChildMatches(check: (VirtualFile) -> Boolean): Boolean {
    if (!this.isDirectory) {
        return false
    }
    var hasMatch = false
    VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Void?>() {
        // good
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                if (check(file)) {
                    hasMatch = true
                }
            }
            return !hasMatch
        }
    })
    return hasMatch
}

/**
 * Checks that no child in virtual file matches predicate
 */
internal fun VirtualFile.noChildMatches(check: (VirtualFile) -> Boolean): Boolean {
    if (!this.isDirectory) {
        return false
    }
    var matchesNone = true
    VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Void?>() {

        // Checks every file until it finds a match
        // If a match is found:
        // - set match to true
        // - stop searching
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                if (check(file)) {
                    matchesNone = false
                }
            }
            return !matchesNone
        }
    })
    return matchesNone
}