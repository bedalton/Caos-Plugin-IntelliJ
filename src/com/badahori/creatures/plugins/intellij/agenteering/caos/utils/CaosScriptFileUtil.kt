package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor


fun CaosScriptFile.copyAsOneLine() {
    ApplicationManager.getApplication().runWriteAction run@{
        val text = text
        var trimmedText = text.split("\n").joinToString(" ") {
            val line = it.trim()
            if (line.startsWith("*"))
                ""
            else
                line
        }
        trimmedText = trimmedText.replace("\\s+".toRegex(), " ")
        trimmedText = trimmedText.replace("\\s*,\\s*".toRegex(), ",").trim()
        copyToClipboard(trimmedText)
    }
}

/**
 * Copies text for BoBCob COB making tool
 * BoBCob does has some issues with this plugins way of formatting code
 * It also does not allow comments in files, so those must be stripped out.
 */
fun copyForBobCob(caosFile:CaosScriptFile) {
    if (caosFile.variant?.isNotOld.orFalse()) {
        copyToClipboard(caosFile.text)
    }
    val lines = caosFile.text.split("[\n,]+".toRegex())
            .mapNotNull { line ->
                line.trim(' ', '\t', '\n',',').let {
                    if (it.startsWith("*"))
                        null
                    else
                        it.nullIfEmpty()
                }
            }
    val value = lines.joinToString("\n")
    copyToClipboard(value)
}

fun CaosScriptFile.trimErrorSpaces() {
    ApplicationManager.getApplication().runWriteAction {
        CommandProcessor.getInstance().executeCommand(project, run@{
            val text = text
            val trimmedText = CaosStringUtil.sanitizeCaosString(text)
            if (trimmedText == text)
                return@run
            val document = document
                    ?: return@run
            document.replaceString(0, text.length, trimmedText)
        }, "Strip Error Spaces", "Strip Error Spaces")
    }
}
