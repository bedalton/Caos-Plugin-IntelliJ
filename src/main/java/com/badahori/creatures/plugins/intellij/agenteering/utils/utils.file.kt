package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.io.File

fun VirtualFile.findChildInSelfOrParent(childName:String, ignoreCase:Boolean, scope: GlobalSearchScope? = null) : VirtualFile? {
    val visited = mutableListOf<VirtualFile>()
    var parent:VirtualFile = this
    while (scope?.contains(parent) != false) {
        parent.findChildRecursive(childName, ignoreCase, visited)?.let {
            return it
        }
        parent = parent.parent
            ?: return null
    }
    return null
}

fun VirtualFile.findChildInSelfOrParent(baseName:String, extensions:List<String>, ignoreCase:Boolean, scope: GlobalSearchScope? = null) : VirtualFile? {
    val visited = mutableListOf<VirtualFile>()
    var parent:VirtualFile = if (this.isDirectory)
        this
    else
        this.parent
    while (scope?.contains(parent) != false) {
        parent.findChildRecursive(baseName, extensions, ignoreCase, visited, scope)?.let {
            return it
        }
        parent = parent.parent
    }
    return null
}

fun VirtualFile.findChildRecursive(childName:String, ignoreCase: Boolean, scope: GlobalSearchScope? = null) : VirtualFile? {
    val visited:MutableList<VirtualFile> = mutableListOf()
    return findChildRecursive(childName, ignoreCase, visited, scope)
}

fun VirtualFile.findChildRecursive(baseName: String, extensions: List<String>, ignoreCase: Boolean, scope: GlobalSearchScope? = null) : VirtualFile? {
    val visited:MutableList<VirtualFile> = mutableListOf()
    return findChildRecursive(baseName, extensions, ignoreCase, visited, scope)
}

private fun VirtualFile.findChildRecursive(childName:String, ignoreCase: Boolean, visited:MutableList<VirtualFile>, scope: GlobalSearchScope? = null) : VirtualFile? {
    if (visited.contains(this)) {
        return null
    }

    // Prevent jumping out when child may or may come back into this folder
    if (!childName.contains("..")) {
        visited.add(this)
    }

    if (scope?.contains(this) == false) {
        return null
    }

    var parent: VirtualFile = this
    if (childName.contains("..")) {
        val components = childName.trim('/').split("[" +File.separator + "/]".toRegex(), limit = 2)
        if (components.size == 2 && components[0] == "..") {
            parent = parent.parent
                ?: return null
            return parent.findChildRecursive(components[1], ignoreCase, visited)
        }
    }

    findChild(childName)?.let { return it }
    val childNameToLower = childName.lowercase()
    for (child in children) {
        if (scope?.contains(child) == false || child in visited)
            continue
        if (child.isDirectory) {
            child.findChildRecursive(childName, ignoreCase, visited, scope)?.let {
                return it
            }
        } else if (childName == child.name) {
            return child
        } else if (ignoreCase && childNameToLower == child.name.lowercase()) {
            return child
        }
    }
    return null
}

private fun VirtualFile.findChildRecursive(baseName:String, extensions: List<String>, ignoreCase: Boolean, visited:MutableList<VirtualFile>, scope: GlobalSearchScope? = null) : VirtualFile? {
    if (!this.isDirectory)
        throw Exception("Cannot find children recursively from non-directory file")
    ProgressIndicatorProvider.checkCanceled()
    if (visited.contains(this))
        return null
    visited.add(this)

    if (scope?.contains(this) == false)
        return null

    val possibleNames = if (ignoreCase) {
        val baseNameLower = baseName.lowercase()
        extensions.map { extension -> "$baseNameLower.${extension.lowercase()}" }
    } else {
        extensions.map { extension -> "$baseName.$extension"}
    }

    for (child in children) {
        if (scope?.contains(child) == false)
            continue
        if (child.isDirectory) {
            child.findChildRecursive(baseName, extensions, ignoreCase, visited, scope)?.let {
                return it
            }
            continue
        }
        val childName = if (ignoreCase) {
            child.name.lowercase()
        } else {
            child.name
        }
        if (childName in possibleNames) {
            return child
        }
    }
    return null
}