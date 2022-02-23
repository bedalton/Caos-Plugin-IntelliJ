package com.badahori.creatures.plugins.intellij.agenteering.utils

import bedalton.creatures.util.pathSeparator
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
            lastIndex > 0 -> lastComponent.substring(0, lastIndex)
            else -> lastComponent // There is no dot
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
        val refined = path.trimEnd(pathSeparatorChar)
        val lastSlash = refined.lastIndexOf(pathSeparatorChar)
        var out = if (lastSlash <= 0)
            refined
        else
            refined.substring(lastSlash)
        out = out.trimStart(pathSeparatorChar)
        return if (keepLastSlash)
            out
        else
            out.trimEnd(pathSeparatorChar)
    }

    @JvmStatic
    fun withoutLastPathComponent(aPath: String): String? {
        var path = aPath
        // Unescape whitespace
        if (path.contains('/') && path.contains('\\')) {
            if (path.contains("\\ ")) {
                path = path.replace("\\ ", " ")
            }
            if (path.contains("\\\t")) {
                path = path.replace("\\\t", "\t")
            }
            // Do not unescape newline, as a path should not have newline in it
        }
        val separator = if (path.contains('/')) {
            if (path.contains('\\')) {
                pathSeparatorChar
            } else {
                '/'
            }
        } else {
            '\\'
        }

        val refined = path.trimEnd(separator)
        if (separator == '\\' && refined.matches("[a-zA-Z][:]".toRegex())) {
            return refined + '\\'
        }
        val lastIndex = refined.lastIndexOf(separator)
        if (lastIndex < 0) {
            return null
        }
        return refined.substring(0, lastIndex + 1)
    }


    @JvmStatic
    fun pathSeparatingLastPathComponent(aPath: String): Pair<String?, String?>? {
        var path = aPath
        // Unescape whitespace
        if (path.contains('/') && path.contains('\\')) {
            if (path.contains("\\ ")) {
                path = path.replace("\\ ", " ")
            }
            if (path.contains("\\\t")) {
                path = path.replace("\\\t", "\t")
            }
            // Do not unescape newline, as a path should not have newline in it
        }

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
        return if (components.size == 1) {
            val component = components.first()
            if (separator == '\\' && component.matches("[a-zA-Z][:]".toRegex())) {
                Pair(component + '\\', null)
            } else {
                Pair(null, components.first())
            }
        } else {
            Pair(components.dropLast(1).joinToString(separator.toString()), components.last())
        }
    }

}