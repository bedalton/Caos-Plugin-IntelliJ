package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.io.File

object FileNameUtils {
    fun getBaseName(fileName:String) : String {
        return File(fileName).name
    }

    fun getExtension(fileName:String) : String {
        return File(fileName).extension
    }
}