package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty

/**
 * Collapses lines quickly and roughly
 */
object CaosScriptsQuickCollapseToLine {
    private val whitespaceRegex = "[, \\t\\n\\r]+".toRegex()
    @Suppress("SpellCheckingInspection")
    // Randomish pattern that hopefully never exists in a users code
    private const val replacePattern = ";;;;__xZZZZx___&&&&_xZZZx__;;;"
    private const val c1eStringPattern = "([ \t]*\\[[^]]+][ \t]*)"
    private const val c2eStringPattern = "([ \t]*\"(\\\\.|[^\\\"])*\"[ \t]*|[ \t]*'(\\\\.|[^\\'])*'[ \t]*)"

    fun collapse(variant:CaosVariant, script: String, collapseChar:CollapseChar = CollapseChar.SPACE): String {
        return if (variant.isOld)
            collapse(c1eStringPattern, script, collapseChar)
        else
            collapse(c2eStringPattern, script, collapseChar)
    }

    private fun collapse(stringPattern:String, caos: String, collapseChar:CollapseChar): String {
        val stringRegex = stringPattern.toRegex()
        val matches = stringRegex.findAll(caos).toList().nullIfEmpty()
        if (matches != null) {
            stringPattern.toRegex().replace(caos, replacePattern)
        }
        val mapper: (string: String) -> String = { string ->
            string.replace(whitespaceRegex, collapseChar.char).trim()
        }
        var out = caos
            .split("\n")
            .filterNot {
                it.isBlank() || it.trim().startsWith("*")
            }
            .joinToString(" ", transform = mapper)

        if (matches != null) {
            val pattern = replacePattern.toRegex()
            for(match in matches) {
                var string = match.value
                val prefix = if (string.startsWith(" ") || string.startsWith("\t")) " " else ""
                val suffix = if (string.endsWith(" ") || string.endsWith("\t")) " " else ""
                out = pattern.replaceFirst(out, prefix + match.value.trim() + suffix)
            }
        }
        return out.trim()
    }

}