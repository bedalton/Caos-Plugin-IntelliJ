package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.io.File

object FileNameUtils {
    @JvmStatic
    fun getBaseName(fileName:String) : String? {
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
    fun getExtension(fileName:String) : String? {
        val lastIndex = fileName.lastIndexOf('.')
        return when {
            lastIndex < 0 -> null
            lastIndex == 0 -> return ""
            lastIndex < fileName.length - 1 -> fileName.substring(lastIndex + 1)
            else -> null
        }
    }
}