package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.findChildInSelfOrParent(childName:String, ignoreCase:Boolean) : VirtualFile? {
    val visited = mutableListOf<VirtualFile>()
    var parent:VirtualFile? = this
    while (parent != null) {
        parent.findChildRecursive(childName, ignoreCase, visited)?.let {
            return it
        }
        parent = parent.parent
    }
    return null
}

fun VirtualFile.findChildInSelfOrParent(baseName:String, extensions:List<String>, ignoreCase:Boolean) : VirtualFile? {
    val visited = mutableListOf<VirtualFile>()
    var parent:VirtualFile? = this
    while (parent != null) {
        parent.findChildRecursive(baseName, extensions, ignoreCase, visited)?.let {
            return it
        }
        parent = parent.parent
    }
    return null
}

fun VirtualFile.findChildRecursive(childName:String, ignoreCase: Boolean) : VirtualFile? {
    val visited:MutableList<VirtualFile> = mutableListOf()
    return findChildRecursive(childName, ignoreCase, visited)
}

fun VirtualFile.findChildRecursive(baseName: String, extensions: List<String>, ignoreCase: Boolean) : VirtualFile? {
    val visited:MutableList<VirtualFile> = mutableListOf()
    return findChildRecursive(baseName, extensions, ignoreCase, visited)
}

private fun VirtualFile.findChildRecursive(childName:String, ignoreCase: Boolean, visited:MutableList<VirtualFile>) : VirtualFile? {
    if (visited.contains(this))
        return null
    visited.add(this)
    findChild(childName)?.let { return it }
    val childNameToLower = childName.toLowerCase()
    for (child in children) {
        if (child.isDirectory) {
            child.findChildRecursive(childName, ignoreCase, visited)?.let {
                return it
            }
        } else if (ignoreCase && child.name.toLowerCase() == childNameToLower) {
            return child
        }
    }
    return null
}

private fun VirtualFile.findChildRecursive(baseName:String, extensions: List<String>, ignoreCase: Boolean, visited:MutableList<VirtualFile>) : VirtualFile? {
    if (visited.contains(this))
        return null
    visited.add(this)
    val childNameToLower = baseName.toLowerCase()
    val potentialNamesLower = extensions.map { extension -> "$childNameToLower.${extension.toLowerCase()}" }
    for (child in children) {
        if (child.isDirectory) {
            child.findChildRecursive(baseName, extensions, ignoreCase, visited)?.let {
                return it
            }
        } else if (ignoreCase && child.name.toLowerCase() in potentialNamesLower) {
            return child
        }
    }
    return null
}