package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.bedalton.common.util.escape

/**
 * Collapses lines quickly and roughly
 */
object CaosScriptsQuickCollapseToLine {
    private val whitespaceRegex = "[, \\t\\n\\r]+".toRegex()
    @Suppress("SpellCheckingInspection")
    // Randomish pattern that hopefully never exists in a users code
    private const val replacePattern = ";;;;__xZZZZx___&&&&___xZZZx__;;;"
    private val c1eStringPattern = "([ \t]*\\[[^]]+][ \t]*)".toRegex(RegexOption.MULTILINE)
    private val c2eStringPattern = "([ \t]*\"(\\\\.|[^\"])*\"[ \t]*)".toRegex()

    fun collapse(variant:CaosVariant, script: String): String {
        return if (variant.isOld)
            collapse(c1eStringPattern, script)
        else
            collapse(c2eStringPattern, script)
    }

    fun collapse(isC1e: Boolean, script: String): String {
        return if (isC1e) {
            collapse(c1eStringPattern, script)
        } else {
            collapse(c2eStringPattern, script)
        }
    }

    private fun collapse(stringRegex:Regex, caosIn: String): String {

        val mapper: (string: String) -> String = { string ->
            string.replace(whitespaceRegex, " ").trim()
        }

        // Strip comments
        var out = caosIn
            .split("\n")
            .filterNot {
                it.isBlank() || it.trim().startsWith("*")
            }
            .joinToString(" ")

        val matches = stringRegex.findAll(out).toList().nullIfEmpty()

        // Replace strings with placeholders
        out = if (matches != null) {
            stringRegex.replace(out, replacePattern)
        } else {
            out
        }.split("\n")
            .joinToString(" ", transform = mapper)

        // Reinsert strings
        if (matches != null) {
            val pattern = replacePattern.toRegex()
            for(match in matches) {
                val stringRaw = match.value
                val prefix = if (stringRaw.startsWith(" ") || stringRaw.startsWith("\t")) " " else ""
                val suffix = if (stringRaw.endsWith(" ") || stringRaw.endsWith("\t")) " " else ""
                val string = stringRaw
                    .trim()
                    .escape()
                out = pattern.replaceFirst(out, prefix + string + suffix)
            }
        }
        return out.trim()
    }
}