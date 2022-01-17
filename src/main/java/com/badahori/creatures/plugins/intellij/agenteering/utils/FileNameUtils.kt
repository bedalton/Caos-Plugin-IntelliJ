package com.badahori.creatures.plugins.intellij.agenteering.utils

import bedalton.creatures.util.pathSeparatorChar
import java.io.File

object FileNameUtils {

    private val anyPathSeparatorRegex = "[\\\\/]".toRegex()

    private const val pathRegex = "([\\\\/][^[\\\\/])[\\\\/]"

    @JvmStatic
    fun getNameWithoutExtension(fileName: String): String? {
        val lastComponent = lastPathComponent(fileName)
        // Handle if directory in hierarchy has a dot in its name
        val lastIndex = lastComponent.lastIndexOf('.')
        return when {
            lastIndex == 0 -> null // dot starts last component
            lastIndex > 0 -> fileName.substring(0, lastIndex)
            else -> fileName
        }
    }

    @JvmStatic
    fun getExtension(fileName: String): String? {
        val lastComponent = lastPathComponent(fileName)
        // Handle if directory in hierarchy has a dot in its name
        val lastIndex = lastComponent.lastIndexOf('.')
        return if (lastIndex < 0) {
            // Has no dot
            null
        } else if (lastIndex + 1 >= lastComponent.lastIndex) {
            // File ends with dot
            ""
        } else {
            lastComponent.substring(lastIndex + 1)
        }
    }

    @JvmStatic
    fun lastPathComponent(path: String, keepLastSlash: Boolean = false): String {
        // Remove trailing slash if any
        val refined = if (keepLastSlash) path else path.trimEnd(pathSeparatorChar)
        val lastSlash = refined.lastIndexOf(File.separatorChar)
        return if (lastSlash <= 0)
            refined
        else
            refined.substring(lastSlash)
    }

    @JvmStatic
    fun withoutLastPathComponent(path: String): String? {
        val separator = if (path.contains('/')) {
            if (path.contains('\\')) {
                pathSeparatorChar
            } else
                '/'
        } else {
            '\\'
        }

        val refined = path.trimEnd(separator)
        val lastIndex = refined.lastIndexOf(separator)
        if (lastIndex < 0) {
            return null
        }
        return refined.substring(0, lastIndex + 1)
    }


    @JvmStatic
    fun pathSeparatingLastPathComponent(path: String): Pair<String?, String>? {
        val separator = if (path.contains('/')) {
            if (path.contains('\\')) {
                pathSeparatorChar
            } else
                '/'
        } else {
            '\\'
        }
        val components = path.split(separator)
        if (components.isEmpty())
            return null
        if (components.size == 1)
            return Pair(null, components.first())
        return Pair(components.dropLast(1).joinToString(separator.toString()), components.last())
    }

}