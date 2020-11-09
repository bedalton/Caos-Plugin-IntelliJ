package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.io.File

object FileNameUtils {
    fun getBaseName(path:String) : String {
        return File(path).nameWithoutExtension
    }
}