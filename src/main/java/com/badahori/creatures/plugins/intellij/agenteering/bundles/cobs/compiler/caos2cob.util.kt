package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe


object Caos2CobUtil {


    /**
     * Regex to parse out sprite file information, including sprite frame number
     * Format FileName.Ext[#]
     * Example: tool.spr[1]
     */
    val ARRAY_ACCESS_REGEX = "^(.*?)\\.(spr|s16|c16)\\[(\\d+)]&"
        .toRegex(RegexOption.IGNORE_CASE)

    /**
     * Regex to parse out sprite file information, including sprite frame number
     * Uses Array access syntax before file extension
     * Format FileName[#].ext
     * Example: tool[1].spr
     */
    val ARRAY_ACCESS_BEFORE_EXTENSION_REGEX = "^(.*?)\\[(\\d+)]\\.(spr|s16|c16)$"
        .toRegex(RegexOption.IGNORE_CASE)

    private val SIMPLE_NAME_REGEX = "^[^\\[]]+\\.(spr|s16|c16)$".toRegex(RegexOption.IGNORE_CASE)


    fun getSpriteFrameInformation(path:String):Pair<String,Int> {
        if (!path.contains('['))
            return Pair(path, 0)
        ARRAY_ACCESS_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
            return Pair("${groupValues[1]}.${groupValues[2]}", groupValues[3].toIntSafe() ?: 0)
        }

        // FileName[#].ext
        ARRAY_ACCESS_BEFORE_EXTENSION_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
            return Pair("${groupValues[1]}.${groupValues[3]}", groupValues[2].toIntSafe() ?: 0)
        }
        throw Caos2CobException("Failed to parse sprite file frame information from text: '$path'")
    }
}