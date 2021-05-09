package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.io.File

object FileNameUtils {
    fun getBaseName(fileName:String) : String? {
        val lastIndex = fileName.lastIndexOf('.')
        return when {
            lastIndex < 0 -> fileName
            lastIndex == 0 -> null
            else -> fileName.substring(0, lastIndex)
        }
    }

    fun getExtension(fileName:String) : String? {
        val lastIndex = fileName.lastIndexOf('.')
        return when {
            lastIndex < 0 -> null
            lastIndex == 0 -> return ""
            else -> fileName.substring(0, lastIndex)
        }
    }
}