package com.badahori.creatures.plugins.intellij.agenteering.utils

import bedalton.creatures.util.pathSeparatorChar
import java.io.File

object FileNameUtils {

    private val anyPathSeparatorRegex = "[\\\\/]".toRegex()

    private val pathRegex = "([\\\\/][^[\\\\/])[\\\\/]"

    @JvmStatic
    fun getNameWithoutExtension(fileName: String): String? {
        var startIndex = fileName.lastIndexOf(File.separatorChar);
        if (startIndex < 0)
            startIndex = 0
        else
            startIndex += 1
        val lastIndex = fileName.lastIndexOf('.')
        return when {
            lastIndex < 0 -> fileName
            lastIndex == 0 -> null
            lastIndex > startIndex -> fileName.substring(startIndex, lastIndex)
            else -> null
        }
    }

    @JvmStatic
    fun getExtension(fileName: String): String? {
        val lastIndex = fileName.lastIndexOf('.')
        return when {
            lastIndex < 0 -> null
            lastIndex == 0 -> return ""
            lastIndex < fileName.length - 1 -> fileName.substring(lastIndex + 1)
            else -> null
        }
    }

    @JvmStatic
    fun lastPathComponent(path: String): String {
        return path.split(anyPathSeparatorRegex).last()
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
        val components = path.split(separator)
        if (components.size < 2)
            return null
        return components.dropLast(1).joinToString(separator.toString())
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