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
    private const val replacePattern = ";;;;__xZZZZx___&&&&___xZZZx__;;;"
    private val c1eStringPattern = "([ \t]*\\[[^\\]]+][ \t]*)".toRegex(RegexOption.MULTILINE)
    private val c2eStringPattern = "([ \t]*\"(\\\\.|[^\\\"])*\"[ \t]*|[ \t]*'(\\\\.|[^\\'])*'[ \t]*)".toRegex()

    fun collapse(variant:CaosVariant, script: String, collapseChar:CollapseChar = CollapseChar.SPACE): String {
        return if (variant.isOld)
            collapse(c1eStringPattern, script, collapseChar)
        else
            collapse(c2eStringPattern, script, collapseChar)
    }

    private fun collapse(stringRegex:Regex, caosIn: String, collapseChar:CollapseChar): String {
        val matches = stringRegex.findAll(caosIn).toList().nullIfEmpty()
        val caos = if (matches != null) {
            stringRegex.replace(caosIn, replacePattern)
        } else
            caosIn
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
                val string = match.value
                val prefix = if (string.startsWith(" ") || string.startsWith("\t")) " " else ""
                val suffix = if (string.endsWith(" ") || string.endsWith("\t")) " " else ""
                out = pattern.replaceFirst(out, prefix + match.value.trim() + suffix)
            }
        }
        return out.trim()
    }
}